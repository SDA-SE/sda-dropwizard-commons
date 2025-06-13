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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;
import org.sdase.commons.server.kafka.consumer.MessageHandler;

class MessageListenerStrategyMetadataContextTest {

  public static final String TOPIC = "topic";

  List<DetachedMetadataContext> handledContexts = new ArrayList<>();

  MessageHandler<String, String> messageHandler =
      (c) -> handledContexts.add(MetadataContext.detachedCurrent());

  MessageListenerStrategy<String, String> messageListenerStrategy =
      new MessageListenerStrategy<>() {
        @Override
        public void processRecords(
            ConsumerRecords<String, String> records, KafkaConsumer<String, String> consumer) {
          for (var consumerRecord : records) {
            try (var ignored = messageHandlerContextFor(consumerRecord)) {
              messageHandler.handle(consumerRecord);
            }
          }
        }

        @Override
        public void verifyConsumerConfig(Map<String, String> config) {}

        @Override
        public void setRetryCounterIfApplicable(long maxRetriesCount) {}
      };

  @Test
  void shouldHandleContexts() {
    messageListenerStrategy.init(Set.of("tenant-id"));
    var recordsMap = new LinkedHashMap<TopicPartition, List<ConsumerRecord<String, String>>>();
    recordsMap.put(
        new TopicPartition(TOPIC, 1),
        List.of(
            consumerRecord("one", "one-value", List.of(Map.entry("tenant-id", "one"))),
            consumerRecord("two", "two-value", List.of(Map.entry("tenant-id", "two")))));
    recordsMap.put(
        new TopicPartition(TOPIC, 2),
        List.of(consumerRecord("three", "three-value", List.of(Map.entry("tenant-id", "three")))));
    var givenRecords = new ConsumerRecords<>(recordsMap);

    //noinspection unchecked
    messageListenerStrategy.processRecords(givenRecords, mock(KafkaConsumer.class));

    assertThat(handledContexts)
        .hasSize(3)
        .extracting(Map.class::cast)
        .containsExactly(
            Map.of("tenant-id", List.of("one")),
            Map.of("tenant-id", List.of("two")),
            Map.of("tenant-id", List.of("three")));
  }

  @Test
  void shouldNotHandleContextsWhenNotConfigured() {
    messageListenerStrategy.init(null);
    var givenRecords =
        new ConsumerRecords<>(
            Map.of(
                new TopicPartition(TOPIC, 1),
                List.of(
                    consumerRecord("one", "one-value", List.of(Map.entry("tenant-id", "one"))))));

    //noinspection unchecked
    messageListenerStrategy.processRecords(givenRecords, mock(KafkaConsumer.class));

    assertThat(handledContexts).hasSize(1).extracting(Map.class::cast).containsExactly(Map.of());
  }

  @Test
  void shouldNormalizeValues() {
    messageListenerStrategy.init(Set.of("tenant-id"));
    var givenRecords =
        new ConsumerRecords<>(
            Map.of(
                new TopicPartition(TOPIC, 1),
                List.of(
                    consumerRecord(
                        "one",
                        "one-value",
                        List.of(
                            Map.entry("tenant-id", "one"),
                            Map.entry("tenant-id", " not normal "),
                            Map.entry("tenant-id", "   "))))));

    //noinspection unchecked
    messageListenerStrategy.processRecords(givenRecords, mock(KafkaConsumer.class));

    assertThat(handledContexts)
        .hasSize(1)
        .extracting(Map.class::cast)
        .containsExactly(Map.of("tenant-id", List.of("one", "not normal")));
  }

  @Test
  void shouldNotAddUnknownFields() {
    messageListenerStrategy.init(Set.of("tenant-id"));
    var givenRecords =
        new ConsumerRecords<>(
            Map.of(
                new TopicPartition(TOPIC, 1),
                List.of(
                    consumerRecord("one", "one-value", List.of(Map.entry("customer-id", "one"))))));

    //noinspection unchecked
    messageListenerStrategy.processRecords(givenRecords, mock(KafkaConsumer.class));

    assertThat(handledContexts)
        .hasSize(1)
        .extracting(Map.class::cast)
        .containsExactly(Map.of("tenant-id", List.of()));
  }

  @Test
  void shouldAddMultipleFields() {
    messageListenerStrategy.init(Set.of("tenant-id", "customer-id"));
    var givenRecords =
        new ConsumerRecords<>(
            Map.of(
                new TopicPartition(TOPIC, 1),
                List.of(
                    consumerRecord(
                        "one",
                        "one-value",
                        List.of(
                            Map.entry("tenant-id", "t-one"), Map.entry("customer-id", "c-one"))))));

    //noinspection unchecked
    messageListenerStrategy.processRecords(givenRecords, mock(KafkaConsumer.class));

    assertThat(handledContexts)
        .hasSize(1)
        .extracting(Map.class::cast)
        .containsExactly(Map.of("tenant-id", List.of("t-one"), "customer-id", List.of("c-one")));
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
