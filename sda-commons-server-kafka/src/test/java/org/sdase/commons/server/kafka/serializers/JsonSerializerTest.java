package org.sdase.commons.server.kafka.serializers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class JsonSerializerTest {

  @Test
  public void testSerializer() {
    SimpleEntity simpleEntity = new SimpleEntity();
    simpleEntity.setName("MyTestName");
    simpleEntity.setLastname("MyTestLastname");

    KafkaJsonSerializer<SimpleEntity> jsonSerializer =
        new KafkaJsonSerializer<>(new ObjectMapper());
    byte[] serialize = jsonSerializer.serialize("123", simpleEntity);

    KafkaJsonDeserializer<SimpleEntity> jsonDeserializer =
        new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class);
    SimpleEntity deserialize = jsonDeserializer.deserialize("123", serialize);

    assertThat(deserialize.getLastname()).isEqualTo(simpleEntity.getLastname());
    assertThat(deserialize.getName()).isEqualTo(simpleEntity.getName());
  }
}
