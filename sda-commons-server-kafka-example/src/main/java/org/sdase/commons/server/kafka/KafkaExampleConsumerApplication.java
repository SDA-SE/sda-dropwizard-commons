package org.sdase.commons.server.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.model.Key;
import org.sdase.commons.server.kafka.model.Value;
import org.sdase.commons.server.kafka.serializers.KafkaJsonDeserializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class KafkaExampleConsumerApplication extends Application<KafkaExampleConfiguration> {


   private final KafkaBundle<KafkaExampleConfiguration> kafka = KafkaBundle.builder().withConfigurationProvider(KafkaExampleConfiguration::getKafka).build();


   public static final String TOPIC_NAME = "exampleTopic";

   private List<Value> receivedMessages = new ArrayList<>();
   private  List<Long> receivedLongs = new ArrayList<>();

   @Override
   public void initialize(Bootstrap<KafkaExampleConfiguration> bootstrap) {
      // Add ConfigurationSubstitutionBundle to allow correct substitution of bootstrap servers in Test case
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(kafka);
   }

   @Override
   public void run(KafkaExampleConfiguration configuration, Environment environment)  {
      ObjectMapper configuredObjectMapper = environment.getObjectMapper();

      createExampleMessageListener(configuredObjectMapper);
      createExampleMessageListenerWithConfiguration();

   }

   private void createExampleMessageListener(ObjectMapper configuredObjectMapper) {
      kafka.registerMessageHandler(
            MessageHandlerRegistration.<Key, Value>builder()
            .withDefaultListenerConfig()
            .forTopic(TOPIC_NAME)
            .withDefaultConsumer()
            .withKeyDeserializer(new KafkaJsonDeserializer<>(configuredObjectMapper, Key.class))
            .withValueDeserializer(new KafkaJsonDeserializer<>(configuredObjectMapper, Value.class))
            .withHandler(record -> receivedMessages.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build()
      );
   }

   private void createExampleMessageListenerWithConfiguration() {
      kafka.registerMessageHandler(
            MessageHandlerRegistration.<Long, Long>builder()
                  .withListenerConfig(ListenerConfig.builder().withPollInterval(1000).build(1))
                  .forTopicConfigs(Collections.singletonList(kafka.getTopicConfiguration("example")))
                  .withConsumerConfig("consumerConfigExample")
                  .withKeyDeserializer(new LongDeserializer())
                  .withValueDeserializer(new LongDeserializer())
                  .withHandler(record -> receivedLongs.add(record.value()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build()
      );
   }

   /**
    * Method just for testing and should not be implemented in real applications.
    */
   List<Value> getReceivedMessages() {
      return receivedMessages;
   }

   /**
    * Method just for testing and should not be implemented in real applications.
    */
   List<Long> getReceivedLongs() {
      return receivedLongs;
   }
}
