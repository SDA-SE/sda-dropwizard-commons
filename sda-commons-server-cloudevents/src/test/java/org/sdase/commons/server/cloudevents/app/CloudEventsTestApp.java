package org.sdase.commons.server.cloudevents.app;

import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.net.URI;
import java.util.Collections;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.sdase.commons.server.cloudevents.CloudEventsBundle;
import org.sdase.commons.server.cloudevents.CloudEventsConsumerHelper;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedEvent;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedMessageHandler;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedPlainMessageHandler;
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

public class CloudEventsTestApp extends Application<CloudEventsTestConfig> {

  private final KafkaBundle<CloudEventsTestConfig> kafkaBundle =
      KafkaBundle.builder().withConfigurationProvider(CloudEventsTestConfig::getKafka).build();

  private final CloudEventsBundle<CloudEventsTestConfig> cloudEventsBundle =
      CloudEventsBundle.<CloudEventsTestConfig>builder().build();

  private PartnerCreatedMessageProducer partnerService;

  private InMemoryStore inMemoryStore;

  @Override
  public void initialize(Bootstrap<CloudEventsTestConfig> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(kafkaBundle);
    bootstrap.addBundle(cloudEventsBundle);
  }

  @Override
  public void run(CloudEventsTestConfig configuration, Environment environment) {
    MessageProducer<String, CloudEvent> partnerCreatedMessageProducer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, CloudEvent>builder()
                .forTopic(kafkaBundle.getTopicConfiguration("partner-created"))
                .withProducerConfig("partner-created")
                .withValueSerializer(new CloudEventSerializer())
                .build());

    partnerService =
        new PartnerCreatedMessageProducer(
            partnerCreatedMessageProducer,
            cloudEventsBundle.createCloudEventsProducerHelper(
                PartnerCreatedEvent.class,
                URI.create("/SDA-SE/partner/partner-example/partner-example-service"),
                "PARTNER_CREATED"));

    inMemoryStore = new InMemoryStore();

    // Scenario 1: Consumer wants the CloudEvent
    CloudEventsConsumerHelper ceConsumer = cloudEventsBundle.createCloudEventsConsumerHelper();
    ContractCreatedMessageHandler contractCreatedMessageHandler =
        new ContractCreatedMessageHandler(ceConsumer, inMemoryStore);
    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withListenerConfig("contract-created")
            .forTopicConfigs(
                Collections.singleton(kafkaBundle.getTopicConfiguration("contract-created")))
            .withConsumerConfig("contract-created")
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(new CloudEventDeserializer())
            .withListenerStrategy(
                new AutocommitMLS<>(contractCreatedMessageHandler, contractCreatedMessageHandler))
            .build());

    // Scenario 2: Consumer only wants the business event
    ContractCreatedPlainMessageHandler contractCreatedPlainMessageHandler =
        new ContractCreatedPlainMessageHandler(inMemoryStore);
    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withListenerConfig("contract-created-plain")
            .forTopicConfigs(
                Collections.singleton(kafkaBundle.getTopicConfiguration("contract-created-plain")))
            .withConsumerConfig("contract-created-plain")
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(
                new KafkaJsonDeserializer<>(
                    environment.getObjectMapper(), ContractCreatedEvent.class))
            .withListenerStrategy(
                new AutocommitMLS<>(
                    contractCreatedPlainMessageHandler, contractCreatedPlainMessageHandler))
            .build());
  }

  public CloudEventsBundle<CloudEventsTestConfig> getCloudEventBundle() {
    return cloudEventsBundle;
  }

  public PartnerCreatedMessageProducer getPartnerService() {
    return partnerService;
  }

  public InMemoryStore getInMemoryStore() {
    return inMemoryStore;
  }
}
