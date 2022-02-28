package org.sdase.commons.server.cloudevents.app;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.Collections;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedEvent;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedMessageHandler;
import org.sdase.commons.server.cloudevents.app.consume.InMemoryStore;
import org.sdase.commons.server.cloudevents.app.produce.PartnerCreatedEvent;
import org.sdase.commons.server.cloudevents.app.produce.PartnerCreatedMessageProducer;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonDeserializer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonSerializer;

public class CloudEventsTestApp extends Application<CloudEventsTestConfig> {

  private final KafkaBundle<CloudEventsTestConfig> kafkaBundle =
      KafkaBundle.builder().withConfigurationProvider(CloudEventsTestConfig::getKafka).build();

  private PartnerCreatedMessageProducer partnerService;

  private InMemoryStore inMemoryStore;

  @Override
  public void initialize(Bootstrap<CloudEventsTestConfig> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(kafkaBundle);
  }

  @Override
  public void run(CloudEventsTestConfig configuration, Environment environment) {
    MessageProducer<String, PartnerCreatedEvent> partnerCreatedMessageProducer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, PartnerCreatedEvent>builder()
                .forTopic(kafkaBundle.getTopicConfiguration("partner-created"))
                .withProducerConfig("partner-created")
                .withValueSerializer(
                    new KafkaJsonSerializer<PartnerCreatedEvent>(environment.getObjectMapper()))
                .build());

    partnerService = new PartnerCreatedMessageProducer(partnerCreatedMessageProducer);

    inMemoryStore = new InMemoryStore();

    ContractCreatedMessageHandler contractCreatedMessageHandler =
        new ContractCreatedMessageHandler(inMemoryStore);
    AutocommitMLS<String, ContractCreatedEvent> strategy =
        new AutocommitMLS<>(contractCreatedMessageHandler, contractCreatedMessageHandler);
    KafkaJsonDeserializer<ContractCreatedEvent> valueDeserializer =
        new KafkaJsonDeserializer<>(environment.getObjectMapper(), ContractCreatedEvent.class);
    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withListenerConfig("contract-created")
            .forTopicConfigs(
                Collections.singleton(kafkaBundle.getTopicConfiguration("contract-created")))
            .withConsumerConfig("contract-created")
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(valueDeserializer)
            .withListenerStrategy(strategy)
            .build());
  }

  public PartnerCreatedMessageProducer getPartnerService() {
    return partnerService;
  }

  public InMemoryStore getInMemoryStore() {
    return inMemoryStore;
  }
}
