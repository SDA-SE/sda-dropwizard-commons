package com.sdase.commons.server.kafka.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class JsonSerializerTest {

   @Test
   public void testSerilizer() {
      SimpleEntity simpleEntity = new SimpleEntity();
      simpleEntity.setName("MyTestName");
      simpleEntity.setLastname("MyTestLastname");

      KafkaJsonSerializer<SimpleEntity> jsonSerializer = new KafkaJsonSerializer<>(new ObjectMapper());
      byte[] serialize = jsonSerializer.serialize("123", simpleEntity);

      KafkaJsonDeserializer<SimpleEntity> jsonDeserializer = new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class);
      SimpleEntity deserialize = jsonDeserializer.deserialize("123", serialize);

      assert(deserialize.getLastname().equals(simpleEntity.getLastname()));
      assert(deserialize.getName().equals(simpleEntity.getName()));

   }

}
