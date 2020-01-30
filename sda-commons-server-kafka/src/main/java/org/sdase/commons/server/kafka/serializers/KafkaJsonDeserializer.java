package org.sdase.commons.server.kafka.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class KafkaJsonDeserializer<T> implements Deserializer<T> {

  private ObjectMapper objectMapper;
  private Class<T> clazz;

  public KafkaJsonDeserializer(ObjectMapper objectMapper, Class<T> clazz) {
    this.objectMapper = objectMapper;
    this.clazz = clazz;
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // no further configuration
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    if (data == null || data.length == 0) {
      return null;
    }

    try {
      return objectMapper.readValue(data, clazz);
    } catch (Exception e) {
      throw new SerializationException(e);
    }
  }

  @Override
  public void close() {
    // not necessary
  }
}
