package com.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.dropwizard.testing.ResourceHelpers;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import com.sdase.commons.server.kafka.config.ListenerConfig;
import com.sdase.commons.server.kafka.consumer.KafkaMessageHandlingException;
import com.sdase.commons.server.kafka.consumer.MessageListener;
import com.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import com.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import com.sdase.commons.server.kafka.exception.ConfigurationException;

import io.dropwizard.testing.junit.DropwizardAppRule;

public class KafkaConsumerCommitBehaviorWithBundleIT extends KafkaBundleConsts {

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("offsets.retention.minutes", "1")
         .withBrokerProperty("offsets.retention.check.interval.ms", "10000");

   private static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaTestApplication.class, ResourceHelpers.resourceFilePath("test-config-default.yml"));

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

   private KafkaBundle<KafkaTestConfiguration> bundle;

   private StringDeserializer deserializer = new StringDeserializer();

   private int numberExceptionThrown = 0;
   private List<String> results = Collections.synchronizedList(new ArrayList<>());


   @Before
   public void setup() {
      KafkaTestConfiguration kafkaTestConfiguration = new KafkaTestConfiguration().withBrokers(KAFKA.getKafkaBrokers());

      // register adhoc implementations
      bundle = KafkaBundle.builder().withConfigurationProvider(KafkaTestConfiguration::getKafka).build();

      bundle.run(kafkaTestConfiguration, DROPWIZARD_APP_RULE.getEnvironment());
      results.clear();
   }

   // Test ignored to speed up integration testing. Waiting for retention must
   // not necessarily tested, since this is default kafka behavior
   // This test might be relevant again, if changes in the @MessageListener with respect to the commit behavior
   // are done
   @Ignore
   @Test
   public void messagesThrowingExceptionsMustBeRetried() throws ConfigurationException { // NOSONAR
      String topic = "messagesThrowingExceptionsMustBeRetried";
      String uuid = UUID.randomUUID().toString();
      KafkaProducer<String, String> producer = KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class);
      producer.send(new ProducerRecord<>(topic, uuid));


      List<MessageListener<String, String>> errorListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withListenerConfig(
                        ListenerConfig.getDefault()
                        )
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> {
                     numberExceptionThrown++;
                     throw new KafkaMessageHandlingException("Error");
                  })
                  .build());

      await().atMost(N_MAX_WAIT_MS, MILLISECONDS).until(() -> numberExceptionThrown == 1);

      assertThat(numberExceptionThrown, equalTo(1));
      errorListener.forEach(MessageListener::stopConsumer);

      // wait for retention of fetch information
      await().atMost(70, SECONDS);

      // reread
      bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .build());

      // wait until consumer is up an running and read data

      await().atMost(N_MAX_WAIT_MS, MILLISECONDS).until(() -> results.size() == 1);

      assertThat("Still only one exception is thrown", numberExceptionThrown, equalTo(1));
      assertThat("handler has reread message", results.size(), is(1));
      assertThat(results, containsInAnyOrder(uuid));
   }

   @Test
   public void committedMessagesMustNotBeRetried() throws ConfigurationException {
      String topic = "committedMessagesMustNotBeRetried";

      List<MessageListener<String, String>> firstListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String>builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .build());


      String uuid = UUID.randomUUID().toString();
      KafkaProducer<String, String> producer = KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class);
      producer.send(new ProducerRecord<>(topic, uuid));

      await().atMost(N_MAX_WAIT_MS, MILLISECONDS).until(() -> results.size() == 1);
      assertThat(results, containsInAnyOrder(uuid));
      firstListener.forEach(MessageListener::stopConsumer);
      results.clear();

      // register new consumer within the same consumer group
      bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .build());

      // wait until consumer is up an running and read data
      await().atMost(N_MAX_WAIT_MS, MILLISECONDS);

      assertThat("There must be no results read by second consumer", results.isEmpty(), is(true));

   }

}
