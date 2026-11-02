package org.sdase.commons.server.kafka.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class KraftKafkaTestResourceTest {
  private static final String TOPIC = "migration-smoke-" + UUID.randomUUID();

  @RegisterExtension
  @Order(0)
  static final KraftKafkaTestResource KAFKA = new KraftKafkaTestResource();

  @RegisterExtension
  @Order(1)
  static final CreateKafkaTopicsClassExtension TOPICS =
      new CreateKafkaTopicsClassExtension(KAFKA, java.util.List.of(TOPIC));

  @Test
  void producesAndConsumesRecords() throws Exception {

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getKafkaConnectString());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
    try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(props)) {
      producer
          .send(new ProducerRecord<>(TOPIC, "hello".getBytes(StandardCharsets.UTF_8)))
          .get(10, TimeUnit.SECONDS);
    }

    var records = KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(TOPIC);
    assertEquals(1, records.size());
    assertEquals("hello", new String(records.getFirst().value(), StandardCharsets.UTF_8));
  }
}
