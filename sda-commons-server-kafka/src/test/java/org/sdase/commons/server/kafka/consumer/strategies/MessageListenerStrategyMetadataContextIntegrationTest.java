package org.sdase.commons.server.kafka.consumer.strategies;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.kafka.clients.consumer.ConsumerRecord.NO_TIMESTAMP;
import static org.apache.kafka.clients.consumer.ConsumerRecord.NULL_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.RetryProcessingErrorMLS;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;

class MessageListenerStrategyMetadataContextIntegrationTest {

  public static final String TOPIC = "topic";

  static List<DetachedMetadataContext> handledContexts = new ArrayList<>();

  static MessageHandler<String, String> messageHandler =
      (c) -> handledContexts.add(MetadataContext.detachedCurrent());

  static MessageHandler<String, String> throwingMessageHandler =
      (c) -> {
        throw new RuntimeException();
      };

  static ErrorHandler<String, String> errorHandler =
      (cr, e, c) -> handledContexts.add(MetadataContext.detachedCurrent());

  @BeforeEach
  void cleanResult() {
    handledContexts.clear();
  }

  static Stream<Arguments> testInstances() {
    return Stream.of(
        // handled in message handler
        Arguments.of(
            "MessageHandler not stopping: AutocommitMLS",
            false,
            new AutocommitMLS<>(messageHandler, errorHandler)),
        Arguments.of(
            "MessageHandler not stopping: RetryProcessingErrorMLS",
            false,
            new RetryProcessingErrorMLS<>(messageHandler, errorHandler)),
        Arguments.of(
            "MessageHandler not stopping: SyncCommitMLS",
            false,
            new SyncCommitMLS<>(messageHandler, errorHandler)),
        // handled in error handler
        Arguments.of(
            "ErrorHandler not stopping: AutocommitMLS",
            false,
            new AutocommitMLS<>(throwingMessageHandler, errorHandler)),
        Arguments.of(
            "ErrorHandler stopping: RetryProcessingErrorMLS",
            true,
            new RetryProcessingErrorMLS<>(throwingMessageHandler, errorHandler)),
        Arguments.of(
            "ErrorHandler not stopping: SyncCommitMLS",
            false,
            new SyncCommitMLS<>(throwingMessageHandler, errorHandler)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testInstances")
  void shouldHandleContexts(
      String testName,
      boolean stopsInTopic,
      MessageListenerStrategy<String, String> messageListenerStrategy) {
    messageListenerStrategy.init(Set.of("tenant-id"));
    messageListenerStrategy.setRetryCounterIfApplicable(3);
    var recordsMap = new LinkedHashMap<TopicPartition, List<ConsumerRecord<String, String>>>();
    recordsMap.put(
        new TopicPartition(TOPIC, 1),
        List.of(
            consumerRecord("one", "one-value", List.of(Map.entry("tenant-id", "one"))),
            consumerRecord("two", "two-value", List.of(Map.entry("tenant-id", "two")))));
    recordsMap.put(
        new TopicPartition(TOPIC, 2),
        List.of(
            consumerRecord("three", "three-value", List.of(Map.entry("tenant-id", "three"))),
            consumerRecord("four", "four-value", List.of(Map.entry("Tenant-Id", "four")))));
    var givenRecords = new ConsumerRecords<>(recordsMap);

    //noinspection unchecked
    messageListenerStrategy.processRecords(givenRecords, mock(KafkaConsumer.class));

    if (stopsInTopic) {
      assertThat(handledContexts)
          .hasSize(2)
          .extracting(Map.class::cast)
          .containsExactly(
              Map.of("tenant-id", List.of("one")), Map.of("tenant-id", List.of("three")));
    } else {
      assertThat(handledContexts)
          .hasSize(4)
          .extracting(Map.class::cast)
          .containsExactly(
              Map.of("tenant-id", List.of("one")),
              Map.of("tenant-id", List.of("two")),
              Map.of("tenant-id", List.of("three")),
              Map.of("tenant-id", List.of("four")));
    }
  }

  private ConsumerRecord<String, String> consumerRecord(
      String key, String value, List<Map.Entry<String, String>> headers) {
    return new ConsumerRecord<>(
        TOPIC,
        1,
        1,
        NO_TIMESTAMP,
        TimestampType.NO_TIMESTAMP_TYPE,
        NULL_SIZE,
        NULL_SIZE,
        key,
        value,
        new RecordHeaders(
            headers.stream()
                .map(e -> new RecordHeader(e.getKey(), e.getValue().getBytes(UTF_8)))
                .collect(Collectors.toList())),
        Optional.empty());
  }
}
