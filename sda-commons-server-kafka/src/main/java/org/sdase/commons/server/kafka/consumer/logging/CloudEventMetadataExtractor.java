package org.sdase.commons.server.kafka.consumer.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;

/** Extracts the identifying CloudEvent attributes from a Kafka message value. */
@FunctionalInterface
public interface CloudEventMetadataExtractor<V> {

  CloudEventMetadata extract(V value);

  /**
   * Reads the CloudEvent attributes from the JSON representation of a structured-mode CloudEvent.
   *
   * @param objectMapper the application's object mapper used to serialize the value
   * @return an extractor for the given mapper
   */
  static <V> CloudEventMetadataExtractor<V> fromJson(ObjectMapper objectMapper) {
    Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    return value -> {
      JsonNode cloudEvent = objectMapper.valueToTree(value);
      return new CloudEventMetadata(
          textOf(cloudEvent, "id"), textOf(cloudEvent, "source"), textOf(cloudEvent, "type"));
    };
  }

  private static String textOf(JsonNode cloudEvent, String attribute) {
    JsonNode value = cloudEvent.get(attribute);
    return value == null || value.isNull() ? null : value.asText();
  }
}
