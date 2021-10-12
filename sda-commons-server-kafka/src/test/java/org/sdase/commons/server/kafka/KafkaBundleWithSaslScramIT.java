package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.SharedKafkaTestResourceScram;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.SaslScramListener;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.common.security.JaasUtils;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.testing.SystemPropertyRule;

public class KafkaBundleWithSaslScramIT {
  private static final CleanupJaasConfigurationRule CLEANUP = new CleanupJaasConfigurationRule();

  private static final SystemPropertyRule PROP =
      new SystemPropertyRule()
          .setProperty(
              JaasUtils.JAVA_LOGIN_CONFIG_PARAM,
              KafkaBundleWithSaslScramIT.class.getResource("/sasl-scram-jaas.conf").getFile());

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResourceScram()
          .registerListener(
              new SaslScramListener().withUsername("kafkaclient").withPassword("client-secret"))
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
          config("kafka.security.user", "kafkaclient"),
          config("kafka.security.password", "client-secret"),
          config("kafka.security.protocol", "SASL_PLAINTEXT"),
          config("kafka.security.saslMechanism", "SCRAM-SHA-512"));

  @ClassRule
  public static final TestRule CHAIN =
      RuleChain.outerRule(CLEANUP).around(PROP).around(KAFKA).around(DW);

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

    Map<byte[], byte[]> records = new HashMap<>();
    records.put(
        "key".getBytes(StandardCharsets.UTF_8), "My Message".getBytes(StandardCharsets.UTF_8));

    KAFKA.getKafkaTestUtils().produceRecords(records, "my-topic", 0);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .untilAsserted(() -> assertThat(results).containsOnly("My Message"));
  }
}
