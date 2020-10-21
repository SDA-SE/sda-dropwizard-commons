package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.salesforce.kafka.test.listeners.SaslPlainListener;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.security.JaasUtils;
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

public class KafkaBundleWithSaslPlainIT {
  private static final CleanupJaasConfigurationRule CLEANUP = new CleanupJaasConfigurationRule();

  private static final SystemPropertyRule PROP =
      new SystemPropertyRule()
          .setProperty(
              JaasUtils.JAVA_LOGIN_CONFIG_PARAM,
              KafkaBundleWithSaslPlainIT.class.getResource("/sasl-jaas.conf").getFile());

  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          .registerListener(
              new SaslPlainListener().withUsername("kafkaclient").withPassword("client-secret"));

  private static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),
          config("kafka.security.user", "kafkaclient"),
          config("kafka.security.password", "client-secret"),
          config("kafka.security.protocol", "SASL_PLAINTEXT"));

  @ClassRule
  public static final TestRule CHAIN =
      RuleChain.outerRule(CLEANUP).around(PROP).around(KAFKA).around(DW);

  @BeforeClass
  public static void beforeClass() {
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
