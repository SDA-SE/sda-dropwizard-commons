package com.sdase.commons.server.kafka;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import com.sdase.commons.server.kafka.builder.ProducerRegistration;
import com.sdase.commons.server.kafka.consumer.MessageListener;
import com.sdase.commons.server.kafka.dropwizard.AppConfiguration;
import com.sdase.commons.server.kafka.dropwizard.KafkaApplication;
import com.sdase.commons.server.kafka.exception.ConfigurationException;
import com.sdase.commons.server.kafka.producer.MessageProducer;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class AppWithoutKafkaServerIT {

   @ClassRule
   public static final DropwizardAppRule<AppConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaApplication.class, ResourceHelpers.resourceFilePath("test-config-con-prod.yml"));


   private List<String> results = Collections.synchronizedList(new ArrayList<>());

   private KafkaBundle<AppConfiguration> bundle;

   @Before
   public void before() {
      KafkaApplication app = DROPWIZARD_APP_RULE.getApplication();
      bundle = app.getKafkaBundle();
      results.clear();
   }

   @Test(expected = TimeoutException.class)
   public void checkMessageListenerCreationThrowsException() throws ConfigurationException {
      String topicName = "checkMessageListenerCreationSuccessful";
      bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String>builder()
                  .withListenerConfig("lc1")
                  .forTopic(topicName)
                  .checkTopicConfiguration()
                  .withDefaultConsumer()
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> results.add(record.value()))
                  .build());

   }

   @Test(expected = TimeoutException.class)
   public void checkProducerWithCreationThrowsException() throws ConfigurationException {
      String topicName = "checkProducerWithCreationThrowsException";
      bundle.registerProducer(ProducerRegistration.builder().forTopic(topicName).createTopicIfMissing().withDefaultProducer().build());
   }

   @Test(expected = TimeoutException.class)
   public void checkProducerWithCheckThrowsException() throws ConfigurationException {
      String topicName = "checkProducerWithCreationThrowsException";
      bundle.registerProducer(ProducerRegistration.builder().forTopic(topicName).checkTopicConfiguration().withDefaultProducer().build());
   }

}
