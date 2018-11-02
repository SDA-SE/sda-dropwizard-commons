package com.sdase.commons.server.kafka;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import com.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import com.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import com.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import com.sdase.commons.server.kafka.config.ListenerConfig;
import com.sdase.commons.server.kafka.consumer.KafkaMessageHandlingException;
import com.sdase.commons.server.kafka.consumer.MessageListener;
import io.dropwizard.testing.junit.DropwizardAppRule;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class KafkaConsumerCommitBehaviorWithBundleIT extends KafkaBundleConsts {

   protected static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("offsets.retention.minutes", "1")
         .withBrokerProperty("offsets.retention.check.interval.ms", "10000");

   protected static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaTestApplication.class);

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

   private KafkaBundle<KafkaTestConfiguration> bundle;

   private StringDeserializer deserializer = new StringDeserializer();

   private int numberExceptionThrown = 0;
   public List<String> results = Collections.synchronizedList(new ArrayList<>());

   private static String TOPIC_2 = TOPIC + "_2";

   @Before
   public void setup() {
      KafkaTestConfiguration kafkaTestConfiguration = new KafkaTestConfiguration().withBrokers(KAFKA.getKafkaBrokers());
      KAFKA.getKafkaTestUtils().createTopic(TOPIC, 1, (short) 1);
      KAFKA.getKafkaTestUtils().createTopic(TOPIC_2, 1, (short) 1);

      // register adhoc implementations
      bundle = KafkaBundle.builder().withConfigurationProvider(KafkaTestConfiguration::getKafka).build();

      bundle.run(kafkaTestConfiguration, DROPWIZARD_APP_RULE.getEnvironment());
      results.clear();
   }

   // Test ignored to speed up integration testing. Waiting for retention must
   // not necessarily tested, since this is default kafka behavior
   @Ignore
   @Test
   public void messagesThrowingExceptionsMustBeRetried() throws InterruptedException {

      String uuid = UUID.randomUUID().toString();
      KafkaProducer<String, String> producer = KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class);
      producer.send(new ProducerRecord<String, String>(TOPIC, uuid));


      List<MessageListener<String, String>> errorListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withListenerConfig(
                        ListenerConfig.getDefault()
                        )
                  .forTopic(TOPIC)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> {
                     numberExceptionThrown++;
                     throw new KafkaMessageHandlingException("Error");
                  })
                  .build());

      int waitCount = 0;
      while (waitCount <= N_MAX_WAIT_MS*4 && numberExceptionThrown < 1) {
         Thread.sleep(N_WAIT_MS);
         waitCount += N_WAIT_MS;
      }

      assertThat(numberExceptionThrown, equalTo(1));
      errorListener.stream().forEach(MessageListener::stopConsumer);

      // wait for retention of fetch information
      Thread.sleep(70000);

      // reread
      bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(TOPIC)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .build());

      // wait until consumer is up an running and read data
      waitCount = 0;
      while (waitCount <= N_MAX_WAIT_MS * 4 && results.size() < 1) {
         Thread.sleep(N_WAIT_MS);
         waitCount += N_WAIT_MS;
      }

      assertThat("Still only one exception is thrown", numberExceptionThrown, equalTo(1));
      assertThat("handler has reread message", results.size(), is(1));
      assertThat(results, containsInAnyOrder(uuid));
   }

   @Test
   public void committedMessagesMustNotBeRetried() throws InterruptedException {

      List<MessageListener<String, String>> firstListener = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String>builder()
                  .withDefaultListenerConfig()
                  .forTopic(TOPIC_2)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .build());


      String uuid = UUID.randomUUID().toString();
      KafkaProducer<String, String> producer = KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class);
      producer.send(new ProducerRecord<String, String>(TOPIC_2, uuid));

      int waitCount = 0;
      while (waitCount <= N_MAX_WAIT_MS * 4 && results.size() < 1) {
         Thread.sleep(N_WAIT_MS);
         waitCount += N_WAIT_MS;
      }

      assertThat(results, containsInAnyOrder(uuid));
      firstListener.forEach(MessageListener::stopConsumer);
      results.clear();

      // register new consumer within the same consumer group
      bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(TOPIC_2)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .build());

      // wait until consumer is up an running and read data
      waitCount = 0;
      while (waitCount <= N_MAX_WAIT_MS * 4 && results.size() < 1) {
         Thread.sleep(N_WAIT_MS);
         waitCount += N_WAIT_MS;
      }

      assertThat("There must be no results read by second consumer", results.isEmpty(), is(true));

   }

}
