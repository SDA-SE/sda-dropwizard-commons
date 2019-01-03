package org.sdase.commons.server.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.confluent.testing.KafkaBrokerEnvironmentRule;
import org.sdase.commons.server.kafka.model.Key;
import org.sdase.commons.server.kafka.model.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class KafkaExampleConsumerIT {

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokers(2);

   private static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE = new KafkaBrokerEnvironmentRule(
         KAFKA);

   private static final DropwizardAppRule<KafkaExampleConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaExampleConsumerApplication.class, ResourceHelpers.resourceFilePath("test-config-consumer.yml"));

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA_BROKER_ENVIRONMENT_RULE).around(DROPWIZARD_APP_RULE);

   @Test
   public void testUseConsumer() throws JsonProcessingException {
      // given
      KafkaExampleConsumerApplication application = DROPWIZARD_APP_RULE.getApplication();
      final String key = "key";
      final String v1 = "v1";
      final String v2 = "v2";

      Map<byte[], byte []> records = new HashMap<>();
      ObjectMapper objectMapper = new ObjectMapper();
      records.put(objectMapper.writeValueAsBytes(new Key(key)), objectMapper.writeValueAsBytes(new Value(v1, v2)));
      KAFKA.getKafkaTestUtils().produceRecords(records, KafkaExampleConsumerApplication.TOPIC_NAME, 0);

      // then
      await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> !application.getReceivedMessages().isEmpty());

      assertThat(application.getReceivedMessages().get(0)).isEqualToComparingFieldByField(new Value(v1, v2));
   }


   @Test
   public void testUseConsumerWithConfiguration() {
      // given
      KafkaExampleConsumerApplication application = DROPWIZARD_APP_RULE.getApplication();

      Map<byte[], byte []> records = new HashMap<>();
      records.put(new byte[] {0,0,0,0,0,0,0,1}, new byte [] {0,0,0,0,0,0,0,2});
      KAFKA.getKafkaTestUtils().produceRecords(records, "exampleTopicConfiguration", 0);

      // then
      await()
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> !application.getReceivedLongs().isEmpty());

      assertThat(application.getReceivedLongs()).contains(2L);
   }


}
