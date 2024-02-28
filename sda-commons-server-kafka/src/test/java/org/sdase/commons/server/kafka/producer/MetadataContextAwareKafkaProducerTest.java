package org.sdase.commons.server.kafka.producer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.utils.Time;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.dropwizard.metadata.MetadataContext;

class MetadataContextAwareKafkaProducerTest {

  static final Set<String> metadataFields = Set.of("tenant-id");

  @SuppressWarnings("unchecked")
  Producer<String, String> delegateProducerMock = mock(Producer.class);

  MetadataContextAwareKafkaProducer<String, String> producer =
      new MetadataContextAwareKafkaProducer<>(delegateProducerMock, metadataFields);

  @AfterEach
  void reset() {
    MetadataContext.createContext(new DetachedMetadataContext());
  }

  @Test
  void shouldNotModifyWithoutConfiguredFields() {
    var noMetadataProducer = new MetadataContextAwareKafkaProducer<>(delegateProducerMock, null);

    var arg1 = new ProducerRecord<>("topic", 1, 10L, "k", "v");
    var result = new CompletableFuture<RecordMetadata>();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ProducerRecord<String, String>> sentRecord =
        ArgumentCaptor.forClass(ProducerRecord.class);

    when(delegateProducerMock.send(sentRecord.capture())).thenReturn(result);

    var actual = noMetadataProducer.send(arg1);

    assertThat(actual).isSameAs(result);
    verify(delegateProducerMock, times(1)).send(arg1);
    assertThat(sentRecord.getValue()).isSameAs(arg1);
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldNotModifyWithoutConfiguredFieldsWithCallback() {
    var noMetadataProducer = new MetadataContextAwareKafkaProducer<>(delegateProducerMock, null);

    var arg1 = new ProducerRecord<>("topic", 1, 10L, "k", "v");
    var result = new CompletableFuture<RecordMetadata>();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ProducerRecord<String, String>> sentRecord =
        ArgumentCaptor.forClass(ProducerRecord.class);
    Callback callback = (m, e) -> {};

    when(delegateProducerMock.send(sentRecord.capture(), eq(callback))).thenReturn(result);

    var actual = noMetadataProducer.send(arg1, callback);

    assertThat(actual).isSameAs(result);
    verify(delegateProducerMock, times(1)).send(arg1, callback);
    assertThat(sentRecord.getValue()).isSameAs(arg1);
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @ParameterizedTest
  @MethodSource("sendTestData")
  void shouldModifyHeadersOnSend(
      Map<String, List<String>> context,
      Map<String, String> headers,
      Map<String, List<String>> expectedHeaders) {
    var givenContext = new DetachedMetadataContext();
    givenContext.putAll(context);
    MetadataContext.createContext(givenContext);

    var givenRecord = new ProducerRecord<>("topic", 1, 10L, "k", "v", toHeaders(headers));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ProducerRecord<String, String>> sentRecord =
        ArgumentCaptor.forClass(ProducerRecord.class);

    var result = new CompletableFuture<RecordMetadata>();

    when(delegateProducerMock.send(sentRecord.capture())).thenReturn(result);

    var actual = producer.send(givenRecord);

    assertThat(actual).isSameAs(result);

    verify(delegateProducerMock, times(1)).send(any());
    verifyNoMoreInteractions(delegateProducerMock);
    assertThat(sentRecord.getValue())
        .extracting(
            ProducerRecord::topic,
            ProducerRecord::partition,
            ProducerRecord::timestamp,
            ProducerRecord::key,
            ProducerRecord::value)
        .contains("topic", 1, 10L, "k", "v");
    assertThat(sentRecord.getValue().headers())
        .extracting(Header::key, h -> new String(h.value(), UTF_8))
        .containsExactlyInAnyOrderElementsOf(
            expectedHeaders.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v -> tuple(e.getKey(), v)))
                .collect(Collectors.toList()));
  }

  @ParameterizedTest
  @MethodSource("sendTestData")
  void shouldModifyHeadersOnSendWithPassedThroughCallback(
      Map<String, List<String>> context,
      Map<String, String> headers,
      Map<String, List<String>> expectedHeaders) {
    var givenContext = new DetachedMetadataContext();
    givenContext.putAll(context);
    MetadataContext.createContext(givenContext);

    var givenRecord = new ProducerRecord<>("topic", 1, 10L, "k", "v", toHeaders(headers));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<ProducerRecord<String, String>> sentRecord =
        ArgumentCaptor.forClass(ProducerRecord.class);

    var result = new CompletableFuture<RecordMetadata>();
    Callback callback = (m, e) -> {};

    when(delegateProducerMock.send(sentRecord.capture(), eq(callback))).thenReturn(result);

    var actual = producer.send(givenRecord, callback);

    assertThat(actual).isSameAs(result);

    verify(delegateProducerMock, times(1)).send(any(), eq(callback));
    verifyNoMoreInteractions(delegateProducerMock);
    assertThat(sentRecord.getValue())
        .extracting(
            ProducerRecord::topic,
            ProducerRecord::partition,
            ProducerRecord::timestamp,
            ProducerRecord::key,
            ProducerRecord::value)
        .contains("topic", 1, 10L, "k", "v");
    assertThat(sentRecord.getValue().headers())
        .extracting(Header::key, h -> new String(h.value(), UTF_8))
        .containsExactlyInAnyOrderElementsOf(
            expectedHeaders.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v -> tuple(e.getKey(), v)))
                .collect(Collectors.toList()));
  }

  private static List<Header> toHeaders(Map<String, String> headers) {
    return headers.entrySet().stream()
        .map(e -> new RecordHeader(e.getKey(), e.getValue().getBytes(UTF_8)))
        .collect(Collectors.toList());
  }

  static Stream<Arguments> sendTestData() {
    // given context, given headers, expected headers
    return Stream.of(
        Arguments.of(
            Map.of("tenant-id", List.of("t1")), Map.of(), Map.of("tenant-id", List.of("t1"))),
        Arguments.of(Map.of("unknown", List.of("unknown-value")), Map.of(), Map.of()),
        Arguments.of(Map.of(), Map.of(), Map.of()),
        Arguments.of(
            Map.of("tenant-id", List.of("t1")),
            Map.of("something", "great"),
            Map.of("something", List.of("great"), "tenant-id", List.of("t1"))),
        Arguments.of(
            Map.of("tenant-id", List.of("t1")),
            Map.of("tenant-id", "custom"),
            Map.of("tenant-id", List.of("custom", "t1"))),
        Arguments.of(
            Map.of("tenant-id", List.of("t1", "  ", " t2 ")),
            Map.of(),
            Map.of("tenant-id", List.of("t1", "t2"))));
  }

  @Test
  void shouldDelegateInitTransactions() {
    producer.initTransactions();
    verify(delegateProducerMock, times(1)).initTransactions();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateBeginTransaction() {
    producer.beginTransaction();
    verify(delegateProducerMock, times(1)).beginTransaction();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateSendOffsetsToTransactionDeprecated() {
    Map<TopicPartition, OffsetAndMetadata> arg1 =
        Map.of(new TopicPartition("topic", 1), mock(OffsetAndMetadata.class));
    String arg2 = "group-id";
    producer.sendOffsetsToTransaction(arg1, arg2);
    //noinspection deprecation
    verify(delegateProducerMock, times(1)).sendOffsetsToTransaction(arg1, arg2);
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateSendOffsetsToTransaction() {
    Map<TopicPartition, OffsetAndMetadata> arg1 =
        Map.of(new TopicPartition("topic", 1), mock(OffsetAndMetadata.class));
    ConsumerGroupMetadata arg2 = mock(ConsumerGroupMetadata.class);
    producer.sendOffsetsToTransaction(arg1, arg2);
    verify(delegateProducerMock, times(1)).sendOffsetsToTransaction(arg1, arg2);
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateCommitTransaction() {
    producer.commitTransaction();
    verify(delegateProducerMock, times(1)).commitTransaction();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateAbortTransaction() {
    producer.abortTransaction();
    verify(delegateProducerMock, times(1)).abortTransaction();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateFlush() {
    producer.flush();
    verify(delegateProducerMock, times(1)).flush();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegatePartitionsFor() {
    var result = List.of(mock(PartitionInfo.class));
    when(delegateProducerMock.partitionsFor("topic")).thenReturn(result);
    var actual = producer.partitionsFor("topic");
    assertThat(actual).isSameAs(result);
    verify(delegateProducerMock, times(1)).partitionsFor("topic");
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateMetrics() {
    MetricName name = new MetricName("m", "g", "d", Map.of());
    Map<MetricName, ? extends Metric> result =
        Map.of(
            name,
            new KafkaMetric(
                new Object(), name, (Measurable) (c, n) -> 1d, new MetricConfig(), Time.SYSTEM));
    when(delegateProducerMock.metrics()).thenAnswer(i -> result);
    var actual = producer.metrics();
    assertThat(actual).isSameAs(result);
    verify(delegateProducerMock, times(1)).metrics();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateClose() {
    producer.close();
    verify(delegateProducerMock, times(1)).close();
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateCloseWithDuration() {
    var arg = Duration.of(1, ChronoUnit.MINUTES);
    producer.close(arg);
    verify(delegateProducerMock, times(1)).close(arg);
    verifyNoMoreInteractions(delegateProducerMock);
  }

  @Test
  void shouldDelegateClientInstanceId() {
    var arg = Duration.of(1, ChronoUnit.MINUTES);
    var givenReturnValue = Uuid.randomUuid();
    when(delegateProducerMock.clientInstanceId(arg)).thenReturn(givenReturnValue);
    var actual = producer.clientInstanceId(arg);
    assertThat(actual).isSameAs(givenReturnValue);
    verify(delegateProducerMock, times(1)).clientInstanceId(arg);
    verifyNoMoreInteractions(delegateProducerMock);
  }
}
