package org.sdase.commons.server.cloudevents;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
  void shouldProduceCloudEvent() {
    String partnerId = UUID.randomUUID().toString();
    app.getPartnerService()
        .produce(new Partner().setId(partnerId).setFirstName("Peter").setLastName("Parker"));

    AtomicReference<PartnerCreatedEvent> eventRef = new AtomicReference<>();
    await()
        .untilAsserted(
            () -> {
              List<ConsumerRecord<byte[], byte[]>> records =
                  KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic("partner-created");
              assertThat(records).isNotEmpty();
              ConsumerRecord<byte[], byte[]> record0 = records.get(0);
              eventRef.set(
                  DW.getObjectMapper().readValue(record0.value(), PartnerCreatedEvent.class));
            });

    PartnerCreatedEvent.PartnerCreated partnerCreatedEvent = eventRef.get().getData();
    assertThat(partnerCreatedEvent.getId()).isEqualTo(partnerId);
  }

  @Test
  void shouldConsumeEvent() throws JsonProcessingException {
    produceContractCreatedEvent("contract-created");

    AtomicReference<ContractCreatedEvent.ContractCreated> eventRef = new AtomicReference<>();
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
    ContractCreatedEvent.ContractCreated event =
        new ContractCreatedEvent.ContractCreated()
            .setContractId("contract-id")
            .setPartnerId("partner-id");
    ContractCreatedEvent cloudEvent =
        (ContractCreatedEvent)
            new ContractCreatedEvent()
                .setData(event)
                .setSource(URI.create("/SDA-SE/contract/contract-foo/test"))
                .setType("CONTRACT_CREATED")
                .setSubject(event.getContractId());

    Map<byte[], byte[]> records = new HashMap<>();
    records.put(
        "123".getBytes(StandardCharsets.UTF_8), DW.getObjectMapper().writeValueAsBytes(cloudEvent));
    KAFKA.getKafkaTestUtils().produceRecords(records, topic, 0);
  }
}
