package org.sdase.commons.server.kafka.consumer.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.helpers.Reporter;

/** Decorates a message handler with a processing log. */
public class KafkaMessageLoggingHandler<K, V> implements MessageHandler<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessageLoggingHandler.class);
  private static final String MISSING_MESSAGE_VALUE = "MissingMessageValue";

  private final String listenerName;
  private final MessageHandler<K, V> delegate;
  private final CloudEventMetadataExtractor<V> cloudEventMetadataExtractor;

  public KafkaMessageLoggingHandler(
      String listenerName,
      MessageHandler<K, V> delegate,
      CloudEventMetadataExtractor<V> cloudEventMetadataExtractor) {
    if (listenerName == null || listenerName.isBlank()) {
      throw new IllegalArgumentException("listenerName must not be blank");
    }
    this.listenerName = listenerName;
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.cloudEventMetadataExtractor =
        Objects.requireNonNull(
            cloudEventMetadataExtractor, "cloudEventMetadataExtractor must not be null");
  }

  /**
   * Creates a logging handler for structured-mode CloudEvents.
   *
   * @param listenerName stable name of the consumer or listener
   * @param delegate the business message handler
   * @param objectMapper the application's object mapper
   * @return a handler that extracts CloudEvent attributes from the JSON representation
   */
  public static <K, V> KafkaMessageLoggingHandler<K, V> forCloudEvents(
      String listenerName, MessageHandler<K, V> delegate, ObjectMapper objectMapper) {
    return new KafkaMessageLoggingHandler<>(
        listenerName, delegate, CloudEventMetadataExtractor.fromJson(objectMapper));
  }

  @Override
  public void handle(ConsumerRecord<K, V> consumerRecord) {
    if (consumerRecord == null) {
      delegate.handle(null);
      return;
    }

    CloudEventMetadata metadata = extractCloudEventMetadata(consumerRecord.value());
    try {
      delegate.handle(consumerRecord);
    } catch (RuntimeException failure) {
      logProcessingAttempt(consumerRecord, metadata, "ERROR", failure.getClass().getName());
      throw failure;
    }

    if (consumerRecord.value() == null) {
      logProcessingAttempt(consumerRecord, metadata, "ERROR", MISSING_MESSAGE_VALUE);
    } else {
      logProcessingAttempt(consumerRecord, metadata, "OK", null);
    }
  }

  private CloudEventMetadata extractCloudEventMetadata(V value) {
    if (value == null) {
      return CloudEventMetadata.EMPTY;
    }
    try {
      return Objects.requireNonNullElse(
          cloudEventMetadataExtractor.extract(value), CloudEventMetadata.EMPTY);
    } catch (RuntimeException failure) {
      warn(
          "Failed to extract CloudEvent metadata for Kafka processing log: {}",
          failure.getClass().getName());
      return CloudEventMetadata.EMPTY;
    }
  }

  private void logProcessingAttempt(
      ConsumerRecord<K, V> consumerRecord,
      CloudEventMetadata metadata,
      String successType,
      String errorType) {
    try {
      if (!LOGGER.isInfoEnabled()) {
        return;
      }
      Map<String, String> fields =
          Map.of(
              "topicName", consumerRecord.topic(),
              "listenerName", listenerName,
              "kafkaMessageKey", Objects.toString(consumerRecord.key(), ""),
              "partition", Integer.toString(consumerRecord.partition()),
              "offset", Long.toString(consumerRecord.offset()),
              "cloudEventId", Objects.toString(metadata.id(), ""),
              "cloudEventSource", Objects.toString(metadata.source(), ""),
              "cloudEventType", Objects.toString(metadata.eventType(), ""),
              "successType", successType,
              "errorType", Objects.toString(errorType, ""));
      Map<String, String> previousValues = new LinkedHashMap<>();
      fields.keySet().forEach(key -> previousValues.put(key, MDC.get(key)));
      try {
        fields.forEach(MDC::put);
        LOGGER.info(
            "Kafka message processed: topicName={}, listenerName={}, kafkaMessageKey={}, partition={}, offset={}, cloudEventId={}, cloudEventSource={}, cloudEventType={}, successType={}, errorType={}",
            fields.get("topicName"),
            fields.get("listenerName"),
            fields.get("kafkaMessageKey"),
            fields.get("partition"),
            fields.get("offset"),
            fields.get("cloudEventId"),
            fields.get("cloudEventSource"),
            fields.get("cloudEventType"),
            fields.get("successType"),
            fields.get("errorType"));
      } finally {
        previousValues.forEach(
            (key, value) -> {
              try {
                if (value == null) {
                  MDC.remove(key);
                } else {
                  MDC.put(key, value);
                }
              } catch (RuntimeException failure) {
                warn("Failed to restore Kafka processing log MDC field: {}", key);
              }
            });
      }
    } catch (RuntimeException failure) {
      warn("Failed to write Kafka processing log: {}", failure.getClass().getName());
    }
  }

  private static void warn(String message, String detail) {
    try {
      LOGGER.warn(message, detail);
    } catch (RuntimeException failure) {
      Reporter.error("Kafka processing log warning failed: " + failure.getClass().getName());
    }
  }
}
