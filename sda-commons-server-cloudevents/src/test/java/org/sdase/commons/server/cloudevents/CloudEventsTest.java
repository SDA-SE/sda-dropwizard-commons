package org.sdase.commons.server.cloudevents;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.cloudevents.app.CloudEventsTestApp;
import org.sdase.commons.server.cloudevents.app.CloudEventsTestConfig;
import org.sdase.commons.server.cloudevents.app.Partner;
import org.sdase.commons.server.cloudevents.app.consume.ContractCreatedEvent;
import org.sdase.commons.server.cloudevents.app.produce.PartnerCreatedEvent;

class CloudEventsTest {

  @Order(0)
  @RegisterExtension
  static SharedKafkaTestResource KAFKA = new SharedKafkaTestResource();

  @Order(1)
  @RegisterExtension
  static DropwizardAppExtension<CloudEventsTestConfig> DW =
      new DropwizardAppExtension<>(
          CloudEventsTestApp.class,
          resourceFilePath("test-config.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString));

  private CloudEventsTestApp app;

  @BeforeAll
  static void beforeAll() {
    KAFKA.getKafkaTestUtils().createTopic("partner-created", 1, (short) 1);
    KAFKA.getKafkaTestUtils().createTopic("contract-created", 1, (short) 1);
  }

  @BeforeEach
  void beforeEach() {
    app = DW.getApplication();
    app.getInMemoryStore().clear();
  }

  @Test
  void shouldProduceCloudEvent() throws Exception {
    String partnerId = UUID.randomUUID().toString();
    app.getPartnerService()
        .produce(new Partner().setId(partnerId).setFirstName("Peter").setLastName("Parker"));

    AtomicReference<CloudEvent> event = new AtomicReference<>();
    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> records =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic("partner-created");
              assertThat(records).isNotEmpty();
              ConsumerRecord<byte[], byte[]> record0 = records.get(0);
              event.set(
                  new CloudEventDeserializer()
                      .deserialize("partner-created", record0.headers(), record0.value()));
            });

    PartnerCreatedEvent partnerCreatedEvent =
        app.getCloudEventBundle()
            .createCloudEventsConsumerHelper()
            .unwrap(event.get(), PartnerCreatedEvent.class);
    assertThat(partnerCreatedEvent.getId()).isEqualTo(partnerId);
  }

  @Test
  void shouldConsumeEvent() throws JsonProcessingException {
    produceContractCreatedEvent("contract-created");

    AtomicReference<ContractCreatedEvent> eventRef = new AtomicReference<>();
    await()
        .untilAsserted(
            () -> {
              assertThat(app.getInMemoryStore().getContractCreatedEvents()).hasSize(1);
              eventRef.set(app.getInMemoryStore().getContractCreatedEvents().get(0));
            });
    assertThat(eventRef.get().getContractId()).isEqualTo("contract-id");
    assertThat(eventRef.get().getPartnerId()).isEqualTo("partner-id");
  }

  @Test
  void shouldConsumePlainEvent() throws JsonProcessingException {
    produceContractCreatedEvent("contract-created-plain");

    AtomicReference<ContractCreatedEvent> eventRef = new AtomicReference<>();
    await()
        .untilAsserted(
            () -> {
              assertThat(app.getInMemoryStore().getContractCreatedEvents()).hasSize(1);
              eventRef.set(app.getInMemoryStore().getContractCreatedEvents().get(0));
            });
    assertThat(eventRef.get().getContractId()).isEqualTo("contract-id");
    assertThat(eventRef.get().getPartnerId()).isEqualTo("partner-id");
  }

  private void produceContractCreatedEvent(String topic) throws JsonProcessingException {
    CloudEventsProducerHelper<ContractCreatedEvent> helper =
        app.getCloudEventBundle()
            .createCloudEventsProducerHelper(
                ContractCreatedEvent.class,
                URI.create("/SDA-SE/contract/contract-foo/test"),
                "CONTRACT_CREATED");
    ContractCreatedEvent event =
        new ContractCreatedEvent().setContractId("contract-id").setPartnerId("partner-id");
    CloudEvent cloudEvent = helper.wrap(event, "partner-id");

    KafkaProducer<String, CloudEvent> producer =
        KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, CloudEventSerializer.class);
    String recordId = UUID.randomUUID().toString();
    producer.send(new ProducerRecord<>(topic, recordId, cloudEvent));
  }
}
