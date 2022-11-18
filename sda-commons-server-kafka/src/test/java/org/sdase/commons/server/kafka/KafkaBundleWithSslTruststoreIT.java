package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.SslListener;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.testing.SystemPropertyClassExtension;

/** A test that uses the JVM-wide truststore to connect to Kafka. */
class KafkaBundleWithSslTruststoreIT {

  @RegisterExtension
  @Order(0)
  private static final SystemPropertyClassExtension PROP =
      new SystemPropertyClassExtension()
          .setProperty(
              "javax.net.ssl.trustStore",
              KafkaBundleWithSslIT.class.getResource("/ssl/kafka.client.truststore.jks").getFile())
          .setProperty("javax.net.ssl.trustStorePassword", "client-password");

  @RegisterExtension
  @Order(1)
  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          // Register and configure SSL authentication on cluster.
          .registerListener(
              new SslListener()
                  .withClientAuthRequested()
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

  @RegisterExtension
  @Order(2)
  private static final DropwizardAppExtension<KafkaTestConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),
          config("kafka.security.protocol", "SSL"));

  @BeforeAll
  static void beforeAll() {
    KAFKA.getKafkaTestUtils().createTopic("my-topic", 1, (short) 1);

    final KafkaProducer<String, String> producer =
        KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);
    producer.send(new ProducerRecord<>("my-topic", "My Message"));
    producer.close();
  }

  @Test
  void shouldReceiveEntries() {
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
