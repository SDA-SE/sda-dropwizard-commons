package org.sdase.commons.server.kafka.serializers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonSerializerTest {

  @Test
  void testSerializer() {
    SimpleEntity simpleEntity = new SimpleEntity();
    simpleEntity.setName("MyTestName");
    simpleEntity.setLastname("MyTestLastname");

    byte[] serialize;
    try (KafkaJsonSerializer<SimpleEntity> jsonSerializer =
        new KafkaJsonSerializer<>(new ObjectMapper())) {
      serialize = jsonSerializer.serialize("123", simpleEntity);
    }

    SimpleEntity deserialize;
    try (KafkaJsonDeserializer<SimpleEntity> jsonDeserializer =
        new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)) {
      deserialize = jsonDeserializer.deserialize("123", serialize);
    }

    assertThat(deserialize.getLastname()).isEqualTo(simpleEntity.getLastname());
    assertThat(deserialize.getName()).isEqualTo(simpleEntity.getName());
  }

  @Test
  void testSerializerWithNullData() {
    byte[] serialize;
    try (KafkaJsonSerializer<SimpleEntity> jsonSerializer =
        new KafkaJsonSerializer<>(new ObjectMapper())) {
      serialize = jsonSerializer.serialize("topic", null);
    }

    assertThat(serialize).isNull();
    SimpleEntity deserialize;
    try (KafkaJsonDeserializer<SimpleEntity> jsonDeserializer =
        new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class)) {
      deserialize = jsonDeserializer.deserialize("123", serialize);
    }

    assertThat(deserialize).isNull();
  }
}
