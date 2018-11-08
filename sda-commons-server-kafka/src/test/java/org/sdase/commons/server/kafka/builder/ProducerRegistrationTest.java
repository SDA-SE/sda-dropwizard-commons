package org.sdase.commons.server.kafka.builder;

import org.sdase.commons.server.kafka.config.ProducerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ProducerRegistrationTest {

   @Test
   public void defaultBuilderHasStringSerializer() {

      ProducerRegistration<String, String> producerRegistration =
            ProducerRegistration.<String, String>builder()
                  .forTopic("TOPIC")
                  .withDefaultProducer()
                  .withKeySerializer(new StringSerializer())
                  .withValueSerializer(new StringSerializer())
                  .build();

      assertThat(producerRegistration, is(notNullValue()));
      assertThat(producerRegistration.getTopic().getTopicName(), equalTo("TOPIC"));
      assertThat(producerRegistration.getKeySerializer(), instanceOf(StringSerializer.class));
      assertThat(producerRegistration.getValueSerializer(), instanceOf(StringSerializer.class));
   }

   @Test
   public void avroBuilderHasAvroSerializer() {

      ProducerRegistration<String, KafkaAvroSerializer> producerRegistration =
            ProducerRegistration.<String, KafkaAvroSerializer>builder()
                  .forTopic("TOPIC")
                  .withDefaultProducer()
                  .withKeySerializer(new StringSerializer())
                  .withAvroValueSerializer()
                  .build();

      assertThat(producerRegistration, is(notNullValue()));
      assertThat(producerRegistration.getTopic().getTopicName(), equalTo("TOPIC"));
      assertThat(producerRegistration.getKeySerializer(), instanceOf(StringSerializer.class));
      assertThat(producerRegistration.getValueSerializer(), instanceOf(KafkaAvroSerializer.class));
   }

   @Test
   public void serializerShouldBeSetCorrectly() {

      ProducerRegistration<Long, Integer> producerRegistration =
            ProducerRegistration.<Long, Integer>builder()
                  .forTopic("TOPIC")
                  .withDefaultProducer()
                  .withKeySerializer(new LongSerializer())
                  .withValueSerializer(new IntegerSerializer())
                  .build();

      assertThat(producerRegistration, is(notNullValue()));
      assertThat(producerRegistration.getTopic().getTopicName(), equalTo("TOPIC"));
      assertThat(producerRegistration.getKeySerializer(), instanceOf(LongSerializer.class));
      assertThat(producerRegistration.getValueSerializer(), instanceOf(IntegerSerializer.class));
   }

   @Test
   public void customProducerCanBeUsed() {

      Map<String, String> test = new HashMap<>();
      test.put("key.serializer", StringSerializer.class.getName());
      test.put("value.serializer", StringSerializer.class.getName());
      test.put("bootstrap.servers", "localhost:9092");
      ProducerConfig producerConfig = new ProducerConfig();
      producerConfig.getConfig().putAll(test);

      ProducerRegistration<String, String> producerRegistration =
            ProducerRegistration.<String, String>builder()
                  .forTopic("TOPIC")
                  .withProducerConfig(producerConfig)
                  .build();

      assertThat(producerRegistration, is(notNullValue()));
      assertThat(producerRegistration.getTopic().getTopicName(), equalTo("TOPIC"));
      assertThat(producerRegistration.getProducerConfig(), equalTo(producerConfig));
   }

}
