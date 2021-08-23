package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.SslListener;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;

/** A test that configures the truststore and keystore in the kafka configuration */
public class KafkaBundleWithSslIT {
  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .registerListener(
              new SslListener()
                  // require client authentication
                  .withClientAuthRequired()
                  .withKeyStoreLocation(resourceFilePath("ssl/kafka.server.keystore.jks"))
                  .withKeyStorePassword("password")
                  .withTrustStoreLocation(resourceFilePath("ssl/kafka.server.truststore.jks"))
                  .withTrustStorePassword("password")
                  .withKeyPassword("password"))
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  private static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),
          config("kafka.security.protocol", "SSL"),

          // configure truststore
          config(
              "kafka.config.ssl\\.truststore\\.location",
              resourceFilePath("ssl/kafka.client.truststore.jks")),
          config("kafka.config.ssl\\.truststore\\.password", "client-password"),

          // configure keystore for client-certificate
          config(
              "kafka.config.ssl\\.keystore\\.location",
              resourceFilePath("ssl/kafka.client.keystore.jks")),
          config("kafka.config.ssl\\.keystore\\.password", "client-password"),
          config("kafka.config.ssl\\.key\\.password", "client-password"));

  @ClassRule public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DW);

  @BeforeClass
  public static void beforeClass() {
    KAFKA.getKafkaTestUtils().createTopic("my-topic", 1, (short) 1);

    final KafkaProducer<String, String> producer =
        KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);
    producer.send(new ProducerRecord<>("my-topic", "My Message"));
    producer.close();
  }

  @Test
  public void shouldReceiveEntries() {
    Set<String> results = new HashSet<>();

    DW.<KafkaTestApplication>getApplication()
        .kafkaBundle()
        .createMessageListener(
            MessageListenerRegistration.builder()
                .withDefaultListenerConfig()
                .forTopic("my-topic")
                .withDefaultConsumer()
                .withKeyDeserializer(new StringDeserializer())
                .withValueDeserializer(new StringDeserializer())
                .withListenerStrategy(new SyncCommitMLS<>(r -> results.add(r.value()), null))
                .build());

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .untilAsserted(() -> assertThat(results).containsOnly("My Message"));
  }
}
