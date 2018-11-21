package org.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sdase.commons.server.kafka.confluent.testing.WrappedSharedKafkaRule;
import org.sdase.commons.server.kafka.serializers.KafkaJsonDeserializer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonSerializer;
import org.sdase.commons.server.kafka.serializers.SimpleEntity;
import org.sdase.commons.server.kafka.confluent.testing.KafkaBrokerEnvironmentRule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.consumer.CallbackMessageHandler;
import org.sdase.commons.server.kafka.consumer.KafkaMessageHandlingException;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.dropwizard.AppConfiguration;
import org.sdase.commons.server.kafka.dropwizard.KafkaApplication;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;

public class KafkaBundleWithConfigIT {

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource();

   private static final DropwizardAppRule<AppConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaApplication.class, ResourceHelpers.resourceFilePath("test-config-con-prod.yml"));

   private static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE = new KafkaBrokerEnvironmentRule(KAFKA);

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA_BROKER_ENVIRONMENT_RULE).around(DROPWIZARD_APP_RULE);

   private List<Long> results = Collections.synchronizedList(new ArrayList<>());
   private List<String> resultsString = Collections.synchronizedList(new ArrayList<>());

   private KafkaBundle<AppConfiguration> kafkaBundle;



   @Before
   public void before() {
      KafkaApplication app = DROPWIZARD_APP_RULE.getApplication();
      kafkaBundle = app.getKafkaBundle();
      results.clear();
      resultsString.clear();
   }

   @Test
   public void allTopicsDescriptionsGenerated() throws ConfigurationException {
      assertThat(kafkaBundle.getTopicConfiguration("topicId1"), is(notNullValue()));
      assertThat(kafkaBundle.getTopicConfiguration("topicId1").getReplicationFactor().count(), is(2));
      assertThat(kafkaBundle.getTopicConfiguration("topicId1").getPartitions().count(), is(2));
      assertThat(kafkaBundle.getTopicConfiguration("topicId1").getProps().size(), is(2));
      assertThat(kafkaBundle.getTopicConfiguration("topicId2"), is(notNullValue()));
   }

   @Test
   public void createProducerWithTopic() throws ConfigurationException {
      MessageProducer<String, String> topicName2 = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<String, String>builder()
                  .forTopic(kafkaBundle.getTopicConfiguration("topicId2"))
                  .createTopicIfMissing()
                  .withDefaultProducer()
                  .withValueSerializer(new StringSerializer())
                  .build());
      assertThat(topicName2, is(notNullValue()));
   }


   @Test
   public void testConsumerCanReadMessages() throws ConfigurationException {
      String topic = "testConsumerCanReadMessages";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<Long, Long> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withConsumerConfig("consumer1")
                  .withHandler(record -> results.add(record.value()))
                  .build());

      MessageProducer<Long, Long> producer = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<Long, Long> builder()
                  .forTopic(topic)
                  .withProducerConfig("producer1")
                  .build());

      // pass in messages
      producer.send(1L, 1L);
      producer.send(2L, 2L);

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> results.size() == 2);
      assertThat(results, containsInAnyOrder(1L, 2L));
   }

   @Test
   public void testConsumerCanReadMessagesNamed() throws ConfigurationException {
      String topic  = "testConsumerCanReadMessagesNamed";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withConsumerConfig("consumer2")
                  .withHandler(record -> resultsString.add(record.value()))
                  .build());

      MessageProducer<String, String> producer = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<String, String> builder()
                  .forTopic(topic)
                  .checkTopicConfiguration()
                  .withProducerConfig("producer2")
                  .build());

      // pass in messages
      producer.send("1l", "1l");
      producer.send("2l", "2l");

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == 2);
      assertThat(resultsString, containsInAnyOrder("1l", "2l"));
   }

   @Test
   public void defaultConProdShouldHaveStringSerializer() throws ConfigurationException {
      String topic = "defaultConProdShouldHaveStringSerializer";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withHandler(record -> resultsString.add(record.value()))
                  .build());

      MessageProducer<String, String> producer = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<String, String> builder()
                  .forTopic(topic)
                  .checkTopicConfiguration()
                  .withDefaultProducer()
                  .build());

      // pass in messages
      producer.send("1l", "1l");
      producer.send("2l", "2l");

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == 2);

      assertThat(resultsString, containsInAnyOrder("1l", "2l"));
   }

   @Test
   public void testKafkaMessages() throws ConfigurationException {
      String topic = "testKafkaMessages";

      List<String> checkMessages = new ArrayList<>();
      KafkaApplication app = DROPWIZARD_APP_RULE.getApplication();
      KAFKA.getKafkaTestUtils().createTopic(topic,1, (short)1);

      app.getKafkaBundle()
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(new StringDeserializer())
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> resultsString.add(record.value()))
                  .build());


      KafkaProducer<String, String> producer = KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);

      // pass in messages
      for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
         String message = UUID.randomUUID().toString();
         checkMessages.add(message);

         producer.send(new ProducerRecord<>(topic, message));
      }

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == checkMessages.size());
      assertThat(resultsString, containsInAnyOrder(checkMessages.toArray()));

   }


   @Test
   public void producerShouldSendMessagesToKafka() throws ConfigurationException {
      String topic = "producerShouldSendMessagesToKafka";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
      MessageProducer<String, String> producer = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<String, String>builder()
                  .forTopic(topic)
                  .withDefaultProducer()
                  .withValueSerializer(new StringSerializer())
                  .build());

      assertThat(producer, notNullValue());

      List<String> messages = new ArrayList<>();
      List<String> receivedMessages = new ArrayList<>();

      for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
         String message = UUID.randomUUID().toString();
         messages.add(message);
         producer.send("test", message);
      }


      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> {
         List<ConsumerRecord<String, String>> consumerRecords = KAFKA
               .getKafkaTestUtils()
               .consumeAllRecordsFromTopic(topic, StringDeserializer.class, StringDeserializer.class);
         consumerRecords.forEach((r) -> receivedMessages.add(r.value()));
         return receivedMessages.size() == messages.size();

      });

      assertThat(receivedMessages.size(), equalTo(KafkaBundleConsts.N_MESSAGES));
      assertThat(receivedMessages, containsInAnyOrder(messages.toArray()));
   }


   @Test
   public void kafkaConsumerReceivesMessages() throws ConfigurationException {

      String topic = "kafkaConsumerReceivesMessages";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);
      StringDeserializer deserializer = new StringDeserializer();

      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(record -> resultsString.add(record.value()))
                  .build());

      // empty topic before test
      KAFKA.getKafkaTestUtils().consumeAllRecordsFromTopic(topic);

      KafkaProducer<String, String> producer = KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);

      List<String> checkMessages = new ArrayList<>();

      // pass in messages
      for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
         String message = UUID.randomUUID().toString();
         checkMessages.add(message);

         producer.send(new ProducerRecord<>(topic, message));
      }

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == checkMessages.size());
      assertThat(resultsString, containsInAnyOrder(checkMessages.toArray()));

   }


   private int callbackCount = 0;

   @Test
   public void kafkaConsumerReceivesMessagesAsyncCommit() throws ConfigurationException {
      String topic = "kafkaConsumerReceivesMessagesAsyncCommit";
      StringDeserializer deserializer = new StringDeserializer();
      KAFKA.getKafkaTestUtils().createTopic(topic,1, (short)1);

      // register adhoc implementations
      assertThat(kafkaBundle, notNullValue());


      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withListenerConfig(
                        ListenerConfig.builder()
                              .withCommitType(MessageListener.CommitType.ASYNC)
                              .useAutoCommitOnly(false)
                              .build(1))
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(new CallbackMessageHandler<String, String>() {
                     @Override
                     public void handleCommitCallback(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                        callbackCount++;
                     }

                     @Override
                     public void handle(ConsumerRecord<String, String> record) throws KafkaMessageHandlingException {
                        resultsString.add(record.value());
                     }
                  })
                  .build());


      KafkaProducer<String, String> producer = KAFKA.getKafkaTestUtils().getKafkaProducer(StringSerializer.class, StringSerializer.class);

      List<String> checkMessages = new ArrayList<>();
      // pass in messages
      for (int i = 0; i < KafkaBundleConsts.N_MESSAGES; i++) {
         String message = UUID.randomUUID().toString();
         checkMessages.add(message);
         producer.send(new ProducerRecord<>(topic, message));
      }

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> callbackCount > 0);

      assertThat(resultsString, containsInAnyOrder(checkMessages.toArray()));
      assertThat(callbackCount, greaterThan(0));

   }


   @Test
   public void multiTest() throws ConfigurationException {

      String TOPIC_CREATE = "create";
      String TOPIC_DELETE = "delete";

      KAFKA.getKafkaTestUtils().createTopic(TOPIC_CREATE, 1, (short) 1);
      KAFKA.getKafkaTestUtils().createTopic(TOPIC_DELETE, 1, (short) 1);


      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<Long, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(TOPIC_CREATE)
                  .withDefaultConsumer()
                  .withKeyDeserializer(new LongDeserializer())
                  .withHandler(record -> resultsString.add(record.value()))
                  .build());

      kafkaBundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(TOPIC_DELETE)
                  .withDefaultConsumer()
                  .withHandler(record -> resultsString.add(record.value()))
                  .build());

      MessageProducer<Long, String> createProducer = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<Long, String> builder()
                  .forTopic(TOPIC_CREATE)
                  .withProducerConfig(new ProducerConfig())
                  .withKeySerializer(new LongSerializer())
                  .build());

      MessageProducer<String, String> deleteProducer = kafkaBundle
            .registerProducer(ProducerRegistration
                  .<String, String> builder()
                  .forTopic(TOPIC_DELETE)
                  .withDefaultProducer()
                  .build());

      createProducer.send(1L, "test1");
      deleteProducer.send("key", "test2");

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == 2);

      assertThat(resultsString, containsInAnyOrder("test1", "test2"));
   }

   @Test
   public void testJsonSerializer() throws ConfigurationException {
      String topic = "testJsonSerializer";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

      kafkaBundle.registerMessageHandler(MessageHandlerRegistration.<String, SimpleEntity>builder()
      .withDefaultListenerConfig()
      .forTopic(topic)
            .withDefaultConsumer()
            .withValueDeserializer(new KafkaJsonDeserializer<>(new ObjectMapper(), SimpleEntity.class))
            .withHandler(x -> resultsString.add(x.value().getName()))
            .build()
      );

      MessageProducer<String, SimpleEntity> prod = kafkaBundle.registerProducer(ProducerRegistration.<String, SimpleEntity>builder()
            .forTopic(topic)
            .withDefaultProducer()
            .withValueSerializer(new KafkaJsonSerializer<>(new ObjectMapper())).build());

      SimpleEntity a = new SimpleEntity();
      a.setName("a");

      SimpleEntity b = new SimpleEntity();
      b.setName("b");


      prod.send("Test", a);
      prod.send("Test", b);

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == 2);

      assertThat(resultsString, containsInAnyOrder("a", "b"));

   }

}
