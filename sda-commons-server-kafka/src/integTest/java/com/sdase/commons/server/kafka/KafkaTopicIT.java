package com.sdase.commons.server.kafka;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import com.github.ftrossbach.club_topicana.core.MismatchedTopicConfigException;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import com.sdase.commons.server.kafka.consumer.MessageListener;
import com.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import com.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import com.sdase.commons.server.kafka.exception.TopicCreationException;
import com.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sdase.commons.server.kafka.KafkaBundleConsts.TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class KafkaTopicIT {

   protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("auto.create.topics.enable", "false")
         .withBrokers(2);
   protected static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaTestApplication.class);

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

   protected KafkaBundle<KafkaTestConfiguration> bundle = KafkaBundle
         .builder()
         .withConfigurationProvider(KafkaTestConfiguration::getKafka)
         .build();

   KafkaTestConfiguration kafkaTestConfiguration = new KafkaTestConfiguration()
         .withBrokers(KAFKA.getKafkaBrokers());


   private List<String> results = Collections.synchronizedList(new ArrayList<>());

   @Before
   public void setup() {
      results.clear();
      bundle.run(kafkaTestConfiguration, DROPWIZARD_APP_RULE.getEnvironment());
   }

   @Test
   public void checkTopicSuccessful() {
      String topicName = "checkTopicSuccessful";
      KAFKA.getKafkaTestUtils().createTopic(topicName, 1, (short) 1);
      List<MessageListener<String, String>> stringStringMessageListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topicName)
                  .checkTopicConfiguration()
                  .withDefaultConsumer()
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> results.add(record.value()))
                  .build());

      assertThat(stringStringMessageListener, is(notNullValue()));

   }

   @Test
   public void checkTopicSuccessfulComplex() {
      String topicName = "checkTopicSuccessfulComplex";
      KAFKA.getKafkaTestUtils().createTopic(topicName, 2, (short) 1);
      List<MessageListener<String, String>> stringStringMessageListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopicConfigs(Collections
                        .singletonList(TopicConfigurationBuilder.builder(topicName)
                              .withPartitionCount(2)
                              .withReplicationFactor(1)
                              .build()))
                  .checkTopicConfiguration()
                  .withDefaultConsumer()
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> results.add(record.value()))
                  .build());

      assertThat(stringStringMessageListener, is(notNullValue()));

   }

   @Test(expected = MismatchedTopicConfigException.class)
   public void checkTopicFails() {

      List<MessageListener<String, String>> stringStringMessageListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic("SomeNotExisting")
                  .checkTopicConfiguration()
                  .withDefaultConsumer()
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> results.add(record.value()))
                  .build());

      assertThat(stringStringMessageListener, is(notNullValue()));

   }

   @Test
   public void createSimpleTopic() {
      String topicName = "createSimpleTopic";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder
            .builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .build();
      bundle.createTopics(Collections.singletonList(topic));
      bundle.checkTopics(Collections.singletonList(topic));
   }

   @Test
   public void createSimpleTopicNameOnly() {
      String topicName = "createSimpleTopicNameOnly";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder.builder(topicName).build();
      ExpectedTopicConfiguration checkTopic = TopicConfigurationBuilder.builder(topicName).withReplicationFactor(1).withPartitionCount(1).build();
      bundle.createTopics(Collections.singletonList(topic));
      bundle.checkTopics(Collections.singletonList(checkTopic));
   }

   @Test(expected = TopicCreationException.class)
   public void createTopicException() {
      String topicName = "createTopicException";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder
            .builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("some.bullshit", "2000")
            .build();
      bundle.createTopics(Collections.singletonList(topic));
      bundle.checkTopics(Collections.singletonList(topic));
   }

   @Test
   public void createComplexTopic() {
      String topicName = "createComplexTopic";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder
            .builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("cleanup.policy", "delete")
            .build();
      bundle.createTopics(Collections.singletonList(topic));
      bundle.checkTopics(Collections.singletonList(topic));
   }


}
