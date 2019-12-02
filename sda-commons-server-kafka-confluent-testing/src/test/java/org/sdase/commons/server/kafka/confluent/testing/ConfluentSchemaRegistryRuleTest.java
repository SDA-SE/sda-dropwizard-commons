package org.sdase.commons.server.kafka.confluent.testing;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;

import io.confluent.kafka.schemaregistry.exceptions.SchemaRegistryException;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;


public class ConfluentSchemaRegistryRuleTest {

   private static SharedKafkaTestResource kafkaTestResource = new SharedKafkaTestResource().withBrokers(2);

   @ClassRule
   public static final ConfluentSchemaRegistryRule SCHEMA_REGISTRY = ConfluentSchemaRegistryRule.builder()
         .withKafkaBrokerRule(new WrappedSharedKafkaRule(kafkaTestResource))
         .build();

   private String exampleSchema = "{\n" +
         "     \"type\": \"record\",\n" +
         "     \"namespace\": \"com.example\",\n" +
         "     \"name\": \"FullName\",\n" +
         "     \"fields\": [\n" +
         "       { \"name\": \"first\", \"type\": \"string\" },\n" +
         "       { \"name\": \"last\", \"type\": \"string\" }\n" +
         "     ]\n" +
         "} \n";


   /**
    * Publishes a message to the Kafka topic with an @{@link io.confluent.kafka.serializers.KafkaAvroSerializer}.
    * During this, the schema is published to the registry internally.
    * Checks if the schema is stored successfully within the registry. Also verifies that the message
    * is within the topic
    */
   @Test
   public void schemaShouldBeRegistered() throws SchemaRegistryException {
      Properties props = new Properties();
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            io.confluent.kafka.serializers.KafkaAvroSerializer.class);
      props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());
      props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaTestResource.getKafkaConnectString());

      KafkaProducer<String, Object> producer = new KafkaProducer<>(props);
      Schema.Parser parser = new Schema.Parser();
      Schema avroSchema = parser.parse(exampleSchema);
      GenericRecord avroRecord = new GenericData.Record(avroSchema);
      avroRecord.put("first", "value1");
      avroRecord.put("last", "value1");
      ProducerRecord<String, Object> record = new ProducerRecord<>("topic1", "test1", avroRecord);
      producer.send(record);
      producer.flush();
      producer.close();

      // Assert that something has been registered
      Iterator<io.confluent.kafka.schemaregistry.client.rest.entities.Schema> allSchemaVersions = SCHEMA_REGISTRY.getAllSchemaVersions("topic1-value");
      Assert.assertTrue(allSchemaVersions.hasNext());

      List<ConsumerRecord<byte[], byte[]>> topic1 = kafkaTestResource.getKafkaTestUtils().consumeAllRecordsFromTopic("topic1");
      Assert.assertFalse(topic1.isEmpty());
   }


}
