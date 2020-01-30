package org.sdase.commons.server.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.kafka.common.serialization.LongSerializer;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.model.Key;
import org.sdase.commons.server.kafka.model.Value;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonSerializer;

public class KafkaExampleProducerApplication extends Application<KafkaExampleConfiguration> {

  private final KafkaBundle<KafkaExampleConfiguration> kafka =
      KafkaBundle.builder().withConfigurationProvider(KafkaExampleConfiguration::getKafka).build();

  /*
   * The message producer can be used within the application
   */
  private MessageProducer<Key, Value> messageProducer;
  private MessageProducer<Long, Long> messageProducerWithConfig;

  @Override
  public void initialize(Bootstrap<KafkaExampleConfiguration> bootstrap) {
    // Add ConfigurationSubstitutionBundle to allow correct substitution of bootstrap servers in
    // Test case
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(kafka);
  }

  @Override
  public void run(KafkaExampleConfiguration configuration, Environment environment) {
    ObjectMapper configuredObjectMapper = environment.getObjectMapper();

    messageProducer = createProducer(configuredObjectMapper);
    messageProducerWithConfig = createProducerWithConfig();
  }

  /**
   * Example 1: create a new producer with default kafka producer and explicitly created serializers
   *
   * @param configuredObjectMapper configured object mapper to serialize JSON
   * @return the producer
   */
  private MessageProducer<Key, Value> createProducer(ObjectMapper configuredObjectMapper) {
    return kafka.registerProducer(
        ProducerRegistration.<Key, Value>builder()
            .forTopic(kafka.getTopicConfiguration("example0"))
            .createTopicIfMissing()
            .withDefaultProducer()
            .withKeySerializer(new KafkaJsonSerializer<>(configuredObjectMapper))
            .withValueSerializer(new KafkaJsonSerializer<>(configuredObjectMapper))
            .build());
  }

  /**
   * Example 2: create a new producer with configured topic and producer
   *
   * @return the producer
   */
  private MessageProducer<Long, Long> createProducerWithConfig() {
    return kafka.registerProducer(
        ProducerRegistration.<Long, Long>builder()
            .forTopic(kafka.getTopicConfiguration("example1"))
            .createTopicIfMissing()
            .withDefaultProducer()
            .withKeySerializer(new LongSerializer())
            .withValueSerializer(new LongSerializer())
            .build());
  }

  /**
   * Method just for testing and should not be implemented in real applications. It shows how a
   * producer can be used
   */
  void sendExample(String key, String value1, String value2) {
    messageProducer.send(new Key(key), new Value(value1, value2));
  }

  /**
   * Method just for testing and should not be implemented in real applications. It shows how a
   * producer can be used
   */
  void sendExampleWithConfiguration(Long key, Long value) {
    messageProducerWithConfig.send(key, value);
  }
}
