package org.sdase.commons.server.kafka.consumer.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.RetryProcessingErrorMLS;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.prometheus.ConsumerTopicMessageHistogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MessageListenerStrategyOnCloseTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MessageListenerStrategyOnCloseTest.class);
  private static final String TOPIC_NAME = "topic";

  @Test
  void shouldCommitLastOffsetOnCloseForAutocommitMLS() {
    final MyMessageHandler messageHandler = new MyMessageHandler();
    shouldCommitLastOffsetOnClose(
        new AutocommitMLS<>(messageHandler, new IgnoreAndProceedErrorHandler<>()), messageHandler);
  }

  @Test
  void shouldCommitLastOffsetOnCloseForSyncCommitMLS() {
    final MyMessageHandler messageHandler = new MyMessageHandler();
    shouldCommitLastOffsetOnClose(
        new SyncCommitMLS<>(messageHandler, new IgnoreAndProceedErrorHandler<>()), messageHandler);
  }

  @Test
  void shouldCommitLastOffsetOnCloseForRetryProcessingErrorMLS() {
    final MyMessageHandler messageHandler = new MyMessageHandler();
    shouldCommitLastOffsetOnClose(
        new RetryProcessingErrorMLS<>(messageHandler, new IgnoreAndProceedErrorHandler<>()),
        messageHandler);
  }

  /**
   * This is a test for the scenario when the application is shutdown while records are being
   * processed. The strategy implementation must make sure that not the offset of the last poll is
   * committed but the offset + 1 of the last record that was actually processed.
   */
  private void shouldCommitLastOffsetOnClose(
      MessageListenerStrategy<String, String> strategy, MyMessageHandler messageHandler) {
    // given
    strategy.init(mock(ConsumerTopicMessageHistogram.class));

    Map<TopicPartition, List<ConsumerRecord<String, String>>> recordsByPartition = new HashMap<>();
    recordsByPartition.put(
        new TopicPartition(TOPIC_NAME, 0),
        Arrays.asList(
            new ConsumerRecord<>(TOPIC_NAME, 0, 0, "key0", "value0"),
            new ConsumerRecord<>(TOPIC_NAME, 0, 1, "key1", "value1"),
            new ConsumerRecord<>(TOPIC_NAME, 0, 2, "key2", "value2")));

    LOGGER.info("Starting thread for strategy");
    final KafkaConsumer<String, String> consumer = mock(KafkaConsumer.class);
    new Thread(
            () -> {
              ConsumerRecords<String, String> records = new ConsumerRecords<>(recordsByPartition);
              strategy.resetOffsetsToCommitOnClose();
              strategy.processRecords(records, consumer);
            })
        .start();

    // remember: we stop the application while record #2 is being processed
    LOGGER.info("Wait for second item to be processed");
    await()
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(
            () -> {
              LOGGER.info(
                  "Checking number of offsets to commit: {}",
                  messageHandler.getProcessedKeys().size());
              // We want to stop in the middle of processing record #2
              assertThat(messageHandler.getProcessedKeys()).hasSize(2);
            });

    // when
    LOGGER.info("Stopping consumer");
    strategy.commitOnClose(consumer);

    // then
    ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> offsets =
        ArgumentCaptor.forClass(Map.class);
    verify(consumer).commitAsync(offsets.capture(), any());

    // two records were processed by the handler
    assertThat(messageHandler.getProcessedKeys()).containsExactlyInAnyOrder("key0", "key1");

    // the only record that was processed successfully is record #1
    // i.e. we expect the offset of record #1 + 1 to be committed
    final Map<TopicPartition, OffsetAndMetadata> actualOffsets = offsets.getValue();
    assertThat(actualOffsets.get(new TopicPartition(TOPIC_NAME, 0)).offset()).isEqualTo(1);
  }

  static class MyMessageHandler implements MessageHandler<String, String> {

    private final Set<String> processedKeys = new HashSet<>();

    @Override
    public void handle(ConsumerRecord<String, String> record) {
      LOGGER.info("Processing record: {}", record.key());
      processedKeys.add(record.key());

      try {
        long sleepFor = 1000L;
        LOGGER.info("Working for {}ms", sleepFor);
        Thread.sleep(sleepFor);
      } catch (InterruptedException e) {
        LOGGER.error("Interrupted!", e);
      }
    }

    public Set<String> getProcessedKeys() {
      return processedKeys;
    }
  }
}
