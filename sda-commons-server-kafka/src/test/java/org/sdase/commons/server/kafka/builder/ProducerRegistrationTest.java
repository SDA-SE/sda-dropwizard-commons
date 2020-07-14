package org.sdase.commons.server.kafka.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.sdase.commons.server.kafka.config.ProducerConfig;

public class ProducerRegistrationTest {

  @Test
  public void defaultBuilderHasStringSerializer() {

    ProducerRegistration<String, String> producerRegistration =
        ProducerRegistration.builder()
            .forTopic("TOPIC")
            .withDefaultProducer()
            .withKeySerializer(new StringSerializer())
            .withValueSerializer(new StringSerializer())
            .build();

    assertThat(producerRegistration).isNotNull();
    assertThat(producerRegistration.getTopic().getTopicName()).isEqualTo("TOPIC");
    assertThat(producerRegistration.getKeySerializer()).isInstanceOf(StringSerializer.class);
    assertThat(producerRegistration.getValueSerializer()).isInstanceOf(StringSerializer.class);
  }

  @Test
  public void serializerShouldBeSetCorrectly() {

    ProducerRegistration<Long, Integer> producerRegistration =
        ProducerRegistration.builder()
            .forTopic("TOPIC")
            .withDefaultProducer()
            .withKeySerializer(new LongSerializer())
            .withValueSerializer(new IntegerSerializer())
            .build();

    assertThat(producerRegistration).isNotNull();
    assertThat(producerRegistration.getTopic().getTopicName()).isEqualTo("TOPIC");
    assertThat(producerRegistration.getKeySerializer()).isInstanceOf(LongSerializer.class);
    assertThat(producerRegistration.getValueSerializer()).isInstanceOf(IntegerSerializer.class);
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

    assertThat(producerRegistration).isNotNull();
    assertThat(producerRegistration.getTopic().getTopicName()).isEqualTo("TOPIC");
    assertThat(producerRegistration.getProducerConfig()).isEqualTo(producerConfig);
  }
}
