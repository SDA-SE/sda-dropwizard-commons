package org.sdase.commons.server.kms.aws.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kms.aws.kafka.dropwizard.AwsEncryptionTestApplication;
import org.sdase.commons.server.kms.aws.kafka.dropwizard.KmsAwsKafkaTestConfiguration;
import org.sdase.commons.server.kms.aws.testing.KmsAwsRule;

public class KmsAwsKafkaBundleIT {

  private static final String PRODUCER = "producer";
  private static final String CONSUMER = "consumer";

  private static final KmsAwsRule KMS_RULE = new KmsAwsRule();
  private static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  private static final DropwizardAppRule<KmsAwsKafkaTestConfiguration> DROPWIZARD_APP_RULE =
      new DropwizardAppRule<>(
          AwsEncryptionTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kmsAws.endpointUrl", KMS_RULE::getEndpointUrl),
          config("kafka.brokers", KAFKA::getKafkaConnectString),
          // performance improvements in the tests
          config("kafka.config.heartbeat\\.interval\\.ms", "250"),
          config("kafka.adminConfig.adminClientRequestTimeoutMs", "30000"));

  private KmsAwsKafkaBundle<KmsAwsKafkaTestConfiguration> kmsAwsKafkaBundle;
  private KafkaBundle<KmsAwsKafkaTestConfiguration> kafkaBundle;

  private final List<String> resultsString = Collections.synchronizedList(new ArrayList<>());

  @ClassRule
  public static final RuleChain CHAIN =
      RuleChain.outerRule(KMS_RULE).around(KAFKA).around(DROPWIZARD_APP_RULE);

  @Before
  public void before() {
    AwsEncryptionTestApplication app = DROPWIZARD_APP_RULE.getApplication();
    kmsAwsKafkaBundle = app.getAwsEncryptionBundle();
    kafkaBundle = app.kafkaBundle();
  }

  @Test
  public void testConsumerCanReadMessagesNamed() {
    String topic = "testConsumerCanReadMessagesNamed";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(CONSUMER)
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(kmsAwsKafkaBundle.wrapDeserializer(new StringDeserializer()))
            .withListenerStrategy(
                new SyncCommitMLS<>(
                    record -> resultsString.add(record.value()),
                    new IgnoreAndProceedErrorHandler<>()))
            .build());

    MessageProducer<String, String> producer =
        kafkaBundle.registerProducer(
            ProducerRegistration.<String, String>builder()
                .forTopic(topic)
                .checkTopicConfiguration()
                .withProducerConfig(PRODUCER)
                .withKeySerializer(new StringSerializer())
                .withValueSerializer(
                    kmsAwsKafkaBundle.wrapSerializer(
                        new StringSerializer(),
                        "arn:aws:kms:eu-central-1:test-account:alias/testing"))
                .build());

    // pass in messages
    producer.send("1l", "1l");
    producer.send("2l", "2l");

    await().atMost(10000, MILLISECONDS).until(() -> resultsString.size() == 2);
    assertThat(resultsString).containsExactlyInAnyOrder("1l", "2l");
  }
}
