package org.sdase.commons.server.kafka.testing;

import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"resource", "unchecked", "ResultOfMethodCallIgnored"})
class KraftKafkaTestUtilsOnlyForCoverageTest {
  private static final String TOPIC = "test-topic";
  private final KraftKafkaTestUtils utils = new KraftKafkaTestUtils("localhost:9092");

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  @Test
  void createTopicHandlesInterruptedException() throws Exception {
    assertCreateTopicFailure(new InterruptedException());
  }

  @Test
  void createTopicHandlesExecutionException() throws Exception {
    assertCreateTopicFailure(new ExecutionException(new IllegalStateException("creation failed")));
  }

  @Test
  void createTopicIgnoresTopicExistsException() throws Exception {
    var admin = mock(Admin.class);
    var result = mock(CreateTopicsResult.class);
    KafkaFuture<Void> future = mock(KafkaFuture.class);
    when(admin.createTopics(any())).thenReturn(result);
    when(result.all()).thenReturn(future);
    when(future.get(20_000L, TimeUnit.MILLISECONDS))
        .thenThrow(new ExecutionException(new TopicExistsException("already exists")));

    try (var adminFactory = mockStatic(Admin.class)) {
      adminFactory.when(() -> Admin.create(any(Properties.class))).thenReturn(admin);

      assertThatNoException().isThrownBy(() -> utils.createTopic(TOPIC, 1, (short) 1));
    }
  }

  @Test
  void createTopicHandlesOtherExceptions() throws Exception {
    assertCreateTopicFailure(new TimeoutException());
  }

  @Test
  void getTopicNamesHandlesInterruptedException() throws Exception {
    assertGetTopicNamesFailure(new InterruptedException());
  }

  @Test
  void getTopicNamesHandlesOtherExceptions() throws Exception {
    assertGetTopicNamesFailure(new TimeoutException());
  }

  @Test
  void produceRecordListHandlesInterruptedException() throws Exception {
    assertProduceRecordsFailure(false, new InterruptedException());
  }

  @Test
  void produceRecordListHandlesOtherExceptions() throws Exception {
    assertProduceRecordsFailure(false, new TimeoutException());
  }

  @Test
  void produceRecordMapHandlesInterruptedException() throws Exception {
    assertProduceRecordsFailure(true, new InterruptedException());
  }

  @Test
  void produceRecordMapHandlesOtherExceptions() throws Exception {
    assertProduceRecordsFailure(true, new TimeoutException());
  }

  private void assertCreateTopicFailure(Exception cause) throws Exception {
    var admin = mock(Admin.class);
    var result = mock(CreateTopicsResult.class);
    KafkaFuture<Void> future = mock(KafkaFuture.class);
    when(admin.createTopics(any())).thenReturn(result);
    when(result.all()).thenReturn(future);
    when(future.get(20_000L, TimeUnit.MILLISECONDS)).thenThrow(cause);

    try (var adminFactory = mockStatic(Admin.class)) {
      adminFactory.when(() -> Admin.create(any(Properties.class))).thenReturn(admin);

      assertThatException().isThrownBy(() -> utils.createTopic(TOPIC, 1, (short) 1));
    }
  }

  private void assertGetTopicNamesFailure(Exception cause) throws Exception {
    var admin = mock(Admin.class);
    var result = mock(ListTopicsResult.class);
    KafkaFuture<Set<String>> future = mock(KafkaFuture.class);
    when(admin.listTopics()).thenReturn(result);
    when(result.names()).thenReturn(future);
    when(future.get(20_000L, TimeUnit.MILLISECONDS)).thenThrow(cause);

    try (var adminFactory = mockStatic(Admin.class)) {
      adminFactory.when(() -> Admin.create(any(Properties.class))).thenReturn(admin);

      assertThatException().isThrownBy(utils::getTopicNames);
    }
  }

  private void assertProduceRecordsFailure(boolean useMap, Exception cause) throws Exception {
    var future = mock(Future.class);
    when(future.get(20_000L, TimeUnit.MILLISECONDS)).thenThrow(cause);

    try (var ignored =
        mockConstruction(
            KafkaProducer.class,
            (producer, context) -> when(producer.send(any())).thenReturn(future))) {
      assertThatException()
          .isThrownBy(
              () -> {
                if (useMap) {
                  utils.produceRecords(Map.of(new byte[] {1}, new byte[] {2}), TOPIC, 0);
                } else {
                  utils.produceRecords(List.of(new ProducerRecord<>(TOPIC, new byte[] {2})));
                }
              });
    }
  }
}
