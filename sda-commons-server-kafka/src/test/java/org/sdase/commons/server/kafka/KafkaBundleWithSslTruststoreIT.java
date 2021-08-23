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
import org.sdase.commons.server.testing.SystemPropertyRule;

/** A test that uses the JVM-wide truststore to connect to Kafka. */
public class KafkaBundleWithSslTruststoreIT {
  private static final CleanupJaasConfigurationRule CLEANUP = new CleanupJaasConfigurationRule();

  private static final SystemPropertyRule PROP =
      new SystemPropertyRule()
          .setProperty(
              "javax.net.ssl.trustStore",
              KafkaBundleWithSslIT.class.getResource("/ssl/kafka.client.truststore.jks").getFile())
          .setProperty("javax.net.ssl.trustStorePassword", "client-password");

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

  private static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),
          config("kafka.security.protocol", "SSL"));

  @ClassRule
  public static final TestRule CHAIN =
      RuleChain.outerRule(CLEANUP).around(PROP).around(KAFKA).around(DW);

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
