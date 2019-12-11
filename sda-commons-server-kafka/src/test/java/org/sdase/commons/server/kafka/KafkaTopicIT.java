package org.sdase.commons.server.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;
import com.github.ftrossbach.club_topicana.core.MismatchedTopicConfigException;
import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.TopicCreationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.kafka.topicana.TopicConfigurationBuilder;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;


public class KafkaTopicIT {

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("auto.create.topics.enable", "false")
         // we only need one consumer offsets partition
         .withBrokerProperty("offsets.topic.num.partitions", "1")
         // we don't need to wait that a consumer group rebalances since we always start with a fresh kafka instance
         .withBrokerProperty("group.initial.rebalance.delay.ms", "0")
         .withBrokers(2);

   private static final LazyRule<DropwizardAppRule<KafkaTestConfiguration>> DROPWIZARD_APP_RULE =new LazyRule<>( () -> DropwizardRuleHelper
         .dropwizardTestAppFrom(KafkaTestApplication.class)
         .withConfigFrom(KafkaTestConfiguration::new)
         .withRandomPorts()
         .withConfigurationModifier(c -> c.getKafka()
               .setBrokers(KAFKA.getKafkaBrokers().stream().map(KafkaBroker::getConnectString).collect(Collectors.toList())))
         .build() );

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);


   private KafkaBundle<KafkaTestConfiguration> bundle;
   private List<String> results = Collections.synchronizedList(new ArrayList<>());

   @Before
   public void setup() {
      results.clear();
      KafkaTestApplication app = DROPWIZARD_APP_RULE.getRule().getApplication();
      bundle = app.kafkaBundle();
   }

   @Test
   public void checkTopicSuccessful()  {
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
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      assertThat(stringStringMessageListener).isNotNull();

   }

   @Test
   public void checkTopicSuccessfulComplex()  {
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
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      assertThat(stringStringMessageListener).isNotNull();

   }

   @Test(expected = MismatchedTopicConfigException.class)
   public void checkTopicFails()  {

      List<MessageListener<String, String>> stringStringMessageListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic("SomeNotExisting")
                  .checkTopicConfiguration()
                  .withDefaultConsumer()
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> results.add(record.value()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      assertThat(stringStringMessageListener).isNotNull();

   }

   @Test
   public void createSimpleTopic() {
      String topicName = "createSimpleTopic";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder
            .builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .build();

      MessageProducer<Object, Object> producer = bundle.registerProducer(ProducerRegistration.builder().forTopic(topic).createTopicIfMissing().withDefaultProducer().build());
      assertThat(producer).isNotNull();
   }

   @Test
   public void createSimpleTopicNameOnly()  {
      String topicName = "createSimpleTopicNameOnly";
      MessageProducer<Object, Object> producer = bundle.registerProducer(ProducerRegistration.builder().forTopic(topicName).createTopicIfMissing().withDefaultProducer().build());
      assertThat(producer).isNotNull();
   }

   @Test(expected = TopicCreationException.class)
   public void createTopicException()  {
      String topicName = "createTopicException";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder
            .builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("some.bullshit", "2000")
            .build();
      bundle.registerProducer(ProducerRegistration.builder().forTopic(topic).createTopicIfMissing().withDefaultProducer().build());
   }

   @Test
   public void createComplexTopic()  {
      String topicName = "createComplexTopic";
      ExpectedTopicConfiguration topic = TopicConfigurationBuilder
            .builder(topicName)
            .withPartitionCount(2)
            .withReplicationFactor(2)
            .withConfig("delete.retention.ms", "2000")
            .withConfig("cleanup.policy", "delete")
            .build();
      MessageProducer<Object, Object> producer = bundle.registerProducer(ProducerRegistration.builder().forTopic(topic).createTopicIfMissing().withDefaultProducer().build());
      assertThat(producer).isNotNull();
   }


}
