package org.sdase.commons.server.kafka.consumer.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class KafkaMessageLoggingHandlerTest {

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private ch.qos.logback.classic.Logger logger;

  @BeforeEach
  void setUp() {
    logger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(KafkaMessageLoggingHandler.class);
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  void shouldLogSuccessfulProcessingAndRestoreMdc() {
    // given
    @SuppressWarnings("unchecked")
    MessageHandler<String, TestEvent> delegate = mock(MessageHandler.class);
    var handler =
        KafkaMessageLoggingHandler.forCloudEvents("listener", delegate, new ObjectMapper());
    var consumerRecord =
        new ConsumerRecord<>(
            "topic", 2, 42, "key", new TestEvent("id", "/source", "created", "secret"));
    MDC.put("topicName", "previous");
    try {
      // when
      handler.handle(consumerRecord);

      // then
      verify(delegate).handle(consumerRecord);
      assertThat(appender.list).hasSize(1);
      var log = appender.list.getFirst();
      assertThat(log.getLevel()).isEqualTo(Level.INFO);
      assertThat(log.getFormattedMessage())
          .isEqualTo(
              "Kafka message processed: topicName=topic, listenerName=listener, kafkaMessageKey=key, partition=2, offset=42, cloudEventId=id, cloudEventSource=/source, cloudEventType=created, successType=OK, errorType=");
      assertThat(log.getMDCPropertyMap())
          .containsAllEntriesOf(
              Map.of(
                  "topicName",
                  "topic",
                  "listenerName",
                  "listener",
                  "kafkaMessageKey",
                  "key",
                  "partition",
                  "2",
                  "offset",
                  "42",
                  "cloudEventId",
                  "id",
                  "cloudEventSource",
                  "/source",
                  "cloudEventType",
                  "created",
                  "successType",
                  "OK",
                  "errorType",
                  ""));
      assertThat(MDC.get("topicName")).isEqualTo("previous");
      assertThat(MDC.get("successType")).isNull();
    } finally {
      MDC.remove("topicName");
    }
  }

  @Test
  void shouldLogEachFailedAttemptAndRethrowOriginalException() {
    // given
    var failure = new IllegalStateException("sensitive exception");
    MessageHandler<String, String> delegate =
        consumerRecord -> {
          throw failure;
        };
    var handler = new KafkaMessageLoggingHandler<>("listener", delegate, value -> null);
    var consumerRecord = new ConsumerRecord<>("topic", 0, 3, "key", "payload");

    // when / then
    assertThatThrownBy(() -> handler.handle(consumerRecord)).isSameAs(failure);
    assertThatThrownBy(() -> handler.handle(consumerRecord)).isSameAs(failure);
    assertThat(appender.list)
        .hasSize(2)
        .allSatisfy(
            log -> {
              assertThat(log.getMDCPropertyMap())
                  .containsEntry("successType", "ERROR")
                  .containsEntry("errorType", IllegalStateException.class.getName());
              assertThat(log.getFormattedMessage())
                  .doesNotContain("sensitive exception", "payload");
            });
  }

  @Test
  void shouldContinueWhenMetadataExtractionFails() {
    // given
    @SuppressWarnings("unchecked")
    MessageHandler<String, String> delegate = mock(MessageHandler.class);
    var handler =
        new KafkaMessageLoggingHandler<>(
            "listener",
            delegate,
            value -> {
              throw new IllegalArgumentException("sensitive");
            });
    var consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", "value");

    // when
    handler.handle(consumerRecord);

    // then
    verify(delegate).handle(consumerRecord);
    assertThat(appender.list).hasSize(2);
    assertThat(appender.list.getFirst().getFormattedMessage())
        .isEqualTo(
            "Failed to extract CloudEvent metadata for Kafka processing log: java.lang.IllegalArgumentException");
    assertThat(appender.list.getLast().getMDCPropertyMap())
        .containsEntry("successType", "OK")
        .containsEntry("cloudEventId", "");
  }

  @Test
  void shouldPreserveDelegateFailureWhenLoggingFails() {
    // given
    var failure = new IllegalStateException("processing");
    MessageHandler<Object, String> delegate =
        consumerRecord -> {
          throw failure;
        };
    var handler = new KafkaMessageLoggingHandler<>("listener", delegate, value -> null);
    Object key =
        new Object() {
          @Override
          public String toString() {
            throw new IllegalArgumentException("sensitive key");
          }
        };
    var consumerRecord = new ConsumerRecord<>("topic", 0, 0, key, "value");

    // when / then
    assertThatThrownBy(() -> handler.handle(consumerRecord)).isSameAs(failure);
    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getFormattedMessage())
        .isEqualTo("Failed to write Kafka processing log: java.lang.IllegalArgumentException");
    assertThat(MDC.get("successType")).isNull();
  }

  @Test
  void shouldPropagateErrorWithoutLoggingIt() {
    // given
    var failure = new AssertionError("processing");
    MessageHandler<String, String> delegate =
        consumerRecord -> {
          throw failure;
        };
    var handler = new KafkaMessageLoggingHandler<>("listener", delegate, value -> null);
    var consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", "value");

    // when / then
    assertThatThrownBy(() -> handler.handle(consumerRecord)).isSameAs(failure);
    assertThat(appender.list).isEmpty();
  }

  @Test
  void shouldRethrowFatalMetadataFailureBeforeInvokingDelegate() {
    // given
    @SuppressWarnings("unchecked")
    MessageHandler<String, String> delegate = mock(MessageHandler.class);
    var failure = new LinkageError("failure");
    var handler =
        new KafkaMessageLoggingHandler<>(
            "listener",
            delegate,
            value -> {
              throw failure;
            });
    var consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", "value");

    // when / then
    assertThatThrownBy(() -> handler.handle(consumerRecord)).isSameAs(failure);
    verifyNoInteractions(delegate);
  }

  @Test
  void shouldLogMissingValueAsErrorAfterDelegateReturns() {
    // given
    @SuppressWarnings("unchecked")
    MessageHandler<String, String> delegate = mock(MessageHandler.class);
    var handler = new KafkaMessageLoggingHandler<>("listener", delegate, value -> null);
    var consumerRecord = new ConsumerRecord<String, String>("topic", 0, 0, "key", null);

    // when
    handler.handle(consumerRecord);

    // then
    verify(delegate).handle(consumerRecord);
    assertThat(appender.list).hasSize(1);
    assertThat(appender.list.getFirst().getMDCPropertyMap())
        .containsEntry("successType", "ERROR")
        .containsEntry("errorType", "MissingMessageValue");
  }

  @Test
  void shouldDelegateNullRecordWithoutLogging() {
    // given
    @SuppressWarnings("unchecked")
    MessageHandler<String, String> delegate = mock(MessageHandler.class);
    var handler = new KafkaMessageLoggingHandler<>("listener", delegate, value -> null);

    // when
    assertThatCode(() -> handler.handle(null)).doesNotThrowAnyException();

    // then
    verify(delegate).handle(null);
    assertThat(appender.list).isEmpty();
  }

  private record TestEvent(String id, String source, String type, String data) {}
}
