package org.sdase.commons.server.kafka.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public class KafkaJsonSerializer<T> implements Serializer<T> {

  private ObjectMapper objectMapper;

  public KafkaJsonSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // no further configuration
  }

  @Override
  public byte[] serialize(String topic, T data) {
    if (data == null) {
      return new byte[0];
    }

    try {
      return objectMapper.writeValueAsBytes(data);
    } catch (Exception e) {
      throw new SerializationException("Error serializing JSON message", e);
    }
  }

  @Override
  public void close() {
    // not necessary
  }
}
