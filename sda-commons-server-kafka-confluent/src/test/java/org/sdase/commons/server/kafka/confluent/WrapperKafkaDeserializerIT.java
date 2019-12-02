package org.sdase.commons.server.kafka.confluent;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.avro.example.FullName;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.config.ProtocolType;
import org.sdase.commons.server.kafka.confluent.builder.WrappedAvroDeserializer;
import org.sdase.commons.server.kafka.confluent.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.confluent.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.confluent.testing.ConfluentSchemaRegistryRule;
import org.sdase.commons.server.kafka.confluent.testing.KafkaBrokerEnvironmentRule;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.producer.MessageProducer;


public class WrapperKafkaDeserializerIT {

    private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource().withBrokers(2);

    private static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE =
        new KafkaBrokerEnvironmentRule(KAFKA);

    private static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
        KafkaTestApplication.class, ResourceHelpers.resourceFilePath("test-config-default.yml"));

    private static final ConfluentSchemaRegistryRule SCHEMA_REGISTRY = ConfluentSchemaRegistryRule.builder()
        .withKafkaProtocol(ProtocolType.PLAINTEXT.toString())
        .withKafkaBrokerRule(KAFKA_BROKER_ENVIRONMENT_RULE)
        .build();

    @ClassRule
    public static final TestRule CHAIN = RuleChain.outerRule(SCHEMA_REGISTRY).around(DROPWIZARD_APP_RULE);


    private List<String> results = Collections.synchronizedList(new ArrayList<>());


    private KafkaBundle<KafkaTestConfiguration> bundle;

    @Before
    public void setup() {
      KafkaTestApplication application = DROPWIZARD_APP_RULE.getApplication();
      //noinspection unchecked
      bundle = application.kafkaBundle();
      results.clear();
    }

    @Test
    public void testWrappedAvroDeserializer()  {
      String topicName = "testWrappedAvroDeserializer";
      KAFKA.getKafkaTestUtils().createTopic(topicName, 1, (short) 1);

      ConsumerConfig config = new ConsumerConfig();
      config.getConfig().put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "true");
      config.getConfig().put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());

      Deserializer<FullName> valueDeserializer = WrappedAvroDeserializer.<FullName>builder()
          .withClassType(FullName.class)
          .withConfigProperties(config.getConfig())
          .build(false);

      List<MessageListener<String, FullName>> stringStringMessageListener = bundle
          .registerMessageHandler(MessageHandlerRegistration
              .<String, FullName> builder()
              .withDefaultListenerConfig()
              .forTopic(topicName)
              .withConsumerConfig(config)
              .withValueDeserializer(valueDeserializer)
              .withHandler(
                  record -> {
                    if (record.value() != null) {
                      results.add(record.value().getFirst());
                    }
                  }
              )
              .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
              .build());

      assertThat(stringStringMessageListener, is(notNullValue()));

      ProducerConfig pConfigAvro = new ProducerConfig();
      pConfigAvro.getConfig().put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
      pConfigAvro.getConfig().put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());

      MessageProducer<String, FullName> producerAvro = bundle.registerProducer(ProducerRegistration.<String, FullName>builder()
          .forTopic(topicName)
          .withProducerConfig(pConfigAvro)
          .build()
      );
      producerAvro.send("test", new FullName("first", "last"));

      ProducerConfig pConfigString = new ProducerConfig();
      pConfigString.getConfig().put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      pConfigString.getConfig().put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY.getConnectionString());

      MessageProducer<String, String> producerString = bundle.registerProducer(ProducerRegistration.<String, String>builder()
          .forTopic(topicName)
          .withProducerConfig(pConfigString)
          .build()
      );
      // send not avro conform record which get ignored
      producerString.send("test", "{ not an avro conform record}");

      producerAvro.send("test", new FullName("second", "last"));

      await().atMost(5, TimeUnit.SECONDS).until(() -> results.size() == 2);
    }

}
