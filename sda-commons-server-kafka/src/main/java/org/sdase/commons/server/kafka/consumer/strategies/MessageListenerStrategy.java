package org.sdase.commons.server.kafka.consumer.strategies;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContextCloseable;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for MessageListener strategies. A strategy defies the way how records are handled and how
 * the consumer interacts with the kafka broker, i.e. the commit handling.
 *
 * <p>A strategy might support the usage of a @{@link MessageHandler} that encapsulates the business
 * logic
 *
 * @param <K> key object type
 * @param <V> value object type
 */
public abstract class MessageListenerStrategy<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageListenerStrategy.class);

  protected ConsumerTopicMessageHistogram consumerProcessedMsgHistogram;

  private Map<TopicPartition, OffsetAndMetadata> offsetsToCommitOnClose = new HashMap<>();
  private Set<String> metadataFields = Set.of();

  /**
   * @param consumerTopicMessageHistogram the histogram that tracks time for consuming single
   *     records
   * @deprecated in favour of {@link #init(ConsumerTopicMessageHistogram, Set)}
   */
  @Deprecated
  @SuppressWarnings({"DeprecatedIsStillUsed", "java:S6355", "java:S1133"})
  public void init(ConsumerTopicMessageHistogram consumerTopicMessageHistogram) {
    init(consumerTopicMessageHistogram, MetadataContext.metadataFields());
  }

  /**
   * @param consumerTopicMessageHistogram the histogram that tracks time for consuming single
   *     records
   * @param metadataFields the configured {@link MetadataContext#metadataFields()}
   */
  public void init(
      ConsumerTopicMessageHistogram consumerTopicMessageHistogram, Set<String> metadataFields) {
    this.consumerProcessedMsgHistogram = consumerTopicMessageHistogram;
    this.metadataFields = Optional.ofNullable(metadataFields).orElse(Set.of());
  }

  /**
   * Implementation of processing and commit logic during poll loop of {@link MessageListener}.
   *
   * <p>The strategy should collect the processing duration metric for each entry of records within
   * the {@link #consumerProcessedMsgHistogram}. Furthermore, each record that was processed
   * successfully should be marked by calling {@link #addOffsetToCommitOnClose} to commit the
   * current offset in case the application shuts down.
   *
   * <pre>
   * SimpleTimer timer = new SimpleTimer();
   * handler.handle(record);
   * addOffsetToCommitOnClose(record);
   *
   * // Prometheus
   * double elapsedSeconds = timer.elapsedSeconds();
   * consumerProcessedMsgHistogram.observe(elapsedSeconds, consumerName, record.topic());
   * </pre>
   *
   * @param records consumer records that should be processed in this poll loop.
   * @param consumer the consumer to communicate with Kafka
   */
  public abstract void processRecords(ConsumerRecords<K, V> records, KafkaConsumer<K, V> consumer);

  /**
   * Creates a context for the current {@link Thread} to handle a message with appropriate
   * information in {@link ThreadLocal}s.
   *
   * <p>Example usage (notice the nested "try" to keep the context for error handling):
   *
   * <pre>
   *   <code>try (var ignored = messageHandlerContextFor(consumerRecord)) {
   *     try {
   *       messageHandler.handle(consumerRecord);
   *     } catch (Exception e) {
   *       errorHandler.handleError(consumerRecord, e, consumer);
   *     }
   *   }
   *   </code>
   * </pre>
   *
   * <p>The context will be set up as follows:
   *
   * <ol>
   *   <li>Sets the {@link MetadataContext} for the current thread to handle the given {@code
   *       consumerContext} with the desired context. The context information is based on the {@link
   *       ConsumerRecord#headers()} considering all configured metadata fields.
   *       <p>This method should be used as try-with-resources around the message handler to
   *       automatically reset the context after processing is done.
   * </ol>
   *
   * @param consumerRecord the consumer record that will be handled
   * @return a {@link java.io.Closeable} that will reset the created contexts to the previous state
   *     on {@link MessageHandlerContextCloseable#close()}
   */
  protected MessageHandlerContextCloseable messageHandlerContextFor(
      ConsumerRecord<K, V> consumerRecord) {
    return MessageHandlerContextCloseable.of(createMetadataContext(consumerRecord));
  }

  private MetadataContextCloseable createMetadataContext(ConsumerRecord<K, V> consumerRecord) {
    var newContext = new DetachedMetadataContext();
    var headers = consumerRecord.headers();
    for (var field : metadataFields) {
      var values =
          StreamSupport.stream(headers.headers(field).spliterator(), false)
              .map(Header::value)
              .map(v -> new String(v, UTF_8))
              .filter(StringUtils::isNotBlank)
              .map(String::trim)
              .collect(Collectors.toList());
      newContext.put(field, values);
    }
    return MetadataContext.createCloseableContext(newContext);
  }

  public void resetOffsetsToCommitOnClose() {
    this.offsetsToCommitOnClose = new HashMap<>();
  }

  protected void addOffsetToCommitOnClose(ConsumerRecord<K, V> consumerRecord) {
    offsetsToCommitOnClose.put(
        new TopicPartition(consumerRecord.topic(), consumerRecord.partition()),
        new OffsetAndMetadata(consumerRecord.offset() + 1, null));
  }

  /**
   * Invoked if the listener shuts down to eventually commit or not commit messages
   *
   * @param consumer the consumer to communicate with Kafka
   */
  public void commitOnClose(KafkaConsumer<K, V> consumer) {
    try {
      if (!offsetsToCommitOnClose.isEmpty()) {
        LOGGER.info("Committing offsets on close: {}", offsetsToCommitOnClose);
        consumer.commitAsync(
            offsetsToCommitOnClose,
            (offsets, e) -> {
              if (e != null) {
                LOGGER.error("Failed to commit on close.", e);
              } else {
                LOGGER.info("Committed offsets on close: {}", offsets);
              }
            });
      }
    } catch (CommitFailedException e) {
      LOGGER.error("Commit failed", e);
    }
  }

  public abstract void verifyConsumerConfig(Map<String, String> config);

  /**
   * Configuration properties that are applied to the {@link ConsumerConfig} of a consumer using
   * this strategy. If these properties are absent in the configuration or configured different, the
   * properties returned by this method must be applied. {@link #verifyConsumerConfig(Map)} may
   * still verify that the forced config is correctly applied or may require further conditions the
   * user has to set intentionally.
   *
   * @return a map with configuration properties that must be applied to {@link
   *     ConsumerConfig#setConfig(Map)} to make the strategy work as expected
   */
  public Map<String, String> forcedConfigToApply() {
    return Collections.emptyMap();
  }
}
