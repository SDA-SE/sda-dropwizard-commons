package org.sdase.commons.server.kafka.consumer.logging;

/** Identifying CloudEvent attributes included in the Kafka message processing log. */
public record CloudEventMetadata(String id, String source, String eventType) {

  static final CloudEventMetadata EMPTY = new CloudEventMetadata(null, null, null);
}
