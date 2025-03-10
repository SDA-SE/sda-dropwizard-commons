package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.SharedKafkaClassExtensionScram;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.SaslScramListener;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.common.security.JaasUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.testing.SystemPropertyClassExtension;

// Disabled specifically for Java 23 to get notified when upgrading to the next latest or finally a
// LTS version. Until then, we want notifications for new issues coming up with latest Java version.
// This test must pass when finally supporting a new Java version with SDA Dropwizard Commons!
@DisabledOnJre(
    value = JRE.JAVA_23,
    disabledReason =
        "Kafka auth mechanism requires SecurityManager https://issues.apache.org/jira/browse/KAFKA-15862")
@ExtendWith(CleanupJaasConfigurationExtension.class)
class KafkaBundleWithSaslScramIT {

  @RegisterExtension
  @Order(1)
  static final SystemPropertyClassExtension PROP =
      new SystemPropertyClassExtension()
          .setProperty(
              JaasUtils.JAVA_LOGIN_CONFIG_PARAM,
              KafkaBundleWithSaslScramIT.class.getResource("/sasl-scram-jaas.conf").getFile());

  @RegisterExtension
  @Order(2)
  static final SharedKafkaTestResource KAFKA =
      new SharedKafkaClassExtensionScram()
          .registerListener(
              new SaslScramListener().withUsername("kafkaclient").withPassword("client-secret"))
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  @RegisterExtension
  @Order(3)
  static final DropwizardAppExtension<KafkaTestConfiguration> DW =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),
          config("kafka.security.user", "kafkaclient"),
          config("kafka.security.password", "client-secret"),
          config("kafka.security.protocol", "SASL_PLAINTEXT"),
          config("kafka.security.saslMechanism", "SCRAM-SHA-512"));

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

    Map<byte[], byte[]> records = new HashMap<>();
    records.put(
        "key".getBytes(StandardCharsets.UTF_8), "My Message".getBytes(StandardCharsets.UTF_8));

    KAFKA.getKafkaTestUtils().produceRecords(records, "my-topic", 0);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .untilAsserted(() -> assertThat(results).containsOnly("My Message"));
  }
}
