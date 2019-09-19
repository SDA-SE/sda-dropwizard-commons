package org.sdase.commons.server.kafka.consumer.strategies;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;

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
public abstract class MessageListenerStrategy<K, V> implements Closeable {

  protected ConsumerTopicMessageHistogram consumerProcessedMsgHistogram;

  public void init(ConsumerTopicMessageHistogram consumerTopicMessageHistogram) {
    this.consumerProcessedMsgHistogram = consumerTopicMessageHistogram;
  }

  /**
   * Implementation of processing and commit logic during poll loop of {@link MessageListener}.
   *
   * <p>The strategy should collect the processing duration metric for each entry of records within
   * the consumerProcessedMsgHistogram.
   *
   * <pre>
   * SimpleTimer timer = new SimpleTimer();
   * handler.handle(record);
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
   * Invoked if the listener shuts down to eventually commit or not commit messages
   *
   * @param consumer the consumer to communicate with Kafka
   */
  public abstract void commitOnClose(KafkaConsumer<K, V> consumer);

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

  @Override
  public void close() {
    // implement if necessary
  }
}
