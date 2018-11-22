package org.sdase.commons.server.kafka;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.avro.example.FullName;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.ProtocolType;
import org.sdase.commons.server.kafka.confluent.testing.ConfluentSchemaRegistryRule;
import org.sdase.commons.server.kafka.confluent.testing.KafkaBrokerEnvironmentRule;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class KafkaAvroIT {

   protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource().withBrokers(2);

   protected static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE =
         new KafkaBrokerEnvironmentRule(KAFKA);

   protected static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaTestApplication.class, ResourceHelpers.resourceFilePath("test-config-default.yml"));


   public static final ConfluentSchemaRegistryRule SCHEMA_REGISTRY = ConfluentSchemaRegistryRule.builder()
         .withKafkaProtocol(ProtocolType.PLAINTEXT.toString())
         .withKafkaBrokerRule(KAFKA_BROKER_ENVIRONMENT_RULE)
         .build();

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(SCHEMA_REGISTRY).around(DROPWIZARD_APP_RULE);


   protected KafkaBundle<KafkaTestConfiguration> bundle = KafkaBundle
         .builder()
         .withConfigurationProvider(KafkaTestConfiguration::getKafka)
         .build();

   KafkaTestConfiguration kafkaTestConfiguration = new KafkaTestConfiguration()
         .withBrokers(KAFKA.getKafkaBrokers());


   private List<String> results = Collections.synchronizedList(new ArrayList<>());


   @Before
   public void setup() {
      results.clear();
      bundle.run(kafkaTestConfiguration, DROPWIZARD_APP_RULE.getEnvironment());
   }

   @Test
   public void shouldWriteAndReadAvro() throws ConfigurationException {
      String topicName = "shouldWriteAndReadAvro";
      KAFKA.getKafkaTestUtils().createTopic(topicName, 1, (short) 1);


      ConsumerConfig config = new ConsumerConfig();
      config.getConfig().put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
      config.getConfig().put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
      config.getConfig().put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());


      List<MessageListener<String, FullName>> stringStringMessageListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, FullName> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topicName)
                  .withConsumerConfig(config)
                  .withHandler(
                        record -> results.add(record.value().getFirst())
                  )
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      assertThat(stringStringMessageListener, is(notNullValue()));

      ProducerConfig pConfig = new ProducerConfig();
      pConfig.getConfig().put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
      pConfig.getConfig().put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());

      MessageProducer<String, FullName> producer = bundle.registerProducer(ProducerRegistration.<String, FullName>builder()
            .forTopic(topicName)
            .withProducerConfig(pConfig)
            .build()
      );

      producer.send("test", new FullName("first", "last"));

      await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 1);






   }



}
