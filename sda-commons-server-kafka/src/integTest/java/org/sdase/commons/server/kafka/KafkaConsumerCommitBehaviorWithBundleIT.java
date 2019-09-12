package org.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder;
import com.salesforce.kafka.test.KafkaBroker;
import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.ProcessingErrorRetryException;
import org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror.RetryProcessingErrorMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class KafkaConsumerCommitBehaviorWithBundleIT extends KafkaBundleConsts {

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource()
         .withBrokerProperty("offsets.retention.minutes", "1")
         .withBrokerProperty("offsets.retention.check.interval.ms", "10000");

   private static final LazyRule<DropwizardAppRule<KafkaTestConfiguration>> DROPWIZARD_APP_RULE = new LazyRule<>(
         () -> DropwizardRuleHelper
               .dropwizardTestAppFrom(KafkaTestApplication.class)
               .withConfigFrom(KafkaTestConfiguration::new)
               .withRandomPorts()
               .withConfigurationModifier(c -> {
                  KafkaConfiguration kafka = c.getKafka();
                  kafka
                        .setBrokers(KAFKA
                              .getKafkaBrokers()
                              .stream()
                              .map(KafkaBroker::getConnectString)
                              .collect(Collectors.toList()));
               })
               .build());


   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

   private StringDeserializer deserializer = new StringDeserializer();

   private int numberExceptionThrown = 0;
   private List<String> results = Collections.synchronizedList(new ArrayList<>());


   private KafkaBundle<KafkaTestConfiguration> bundle = ((KafkaTestApplication) DROPWIZARD_APP_RULE.getRule().getApplication()).kafkaBundle();

   @Before
   public void setup() {
      results.clear();
   }

   // Test ignored to speed up integration testing. Waiting for retention must
   // not necessarily tested, since this is default kafka behavior
   // This test might be relevant again, if changes in the @MessageListener with respect to the commit behavior
   // are done
   @Ignore
   @Test
   public void messagesThrowingExceptionsMustBeRetried()  { // NOSONAR
      String topic = "messagesThrowingExceptionsMustBeRetried";
      String uuid = UUID.randomUUID().toString();
      KafkaProducer<String, String> producer = KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class);
      producer.send(new ProducerRecord<>(topic, uuid));


      List<MessageListener<String, String>> errorListener = bundle
            .registerMessageHandler(MessageHandlerRegistration // NOSONAR
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
                     throw new RuntimeException("Error"); // NOSONAR
                  })
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      await().atMost(N_MAX_WAIT_MS, MILLISECONDS).until(() -> numberExceptionThrown == 1);

      assertThat(numberExceptionThrown).isEqualTo(1);
      errorListener.forEach(MessageListener::stopConsumer);

      // wait for retention of fetch information
      await().atMost(70, SECONDS);

      // reread
      bundle
            .registerMessageHandler(MessageHandlerRegistration // NOSONAR
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      // wait until consumer is up an running and read data

      await().atMost(N_MAX_WAIT_MS, MILLISECONDS).until(() -> results.size() == 1);

      assertThat(numberExceptionThrown).withFailMessage("Still only one exception is thrown").isEqualTo(1);
      assertThat(results).withFailMessage("handler has reread message").hasSize(1);
      assertThat(results).containsExactlyInAnyOrder(uuid);
   }

   @Test
   public void committedMessagesMustNotBeRetried() {
      String topic = "committedMessagesMustNotBeRetried";

      List<MessageListener<String, String>> firstListener = bundle
            .registerMessageHandler(MessageHandlerRegistration // NOSONAR
                  .<String, String>builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());


      String uuid = UUID.randomUUID().toString();
      KafkaProducer<String, String> producer = KAFKA
            .getKafkaTestUtils()
            .getKafkaProducer(StringSerializer.class, StringSerializer.class);
      producer.send(new ProducerRecord<>(topic, uuid));

      await().atMost(N_MAX_WAIT_MS, MILLISECONDS).until(() -> results.size() == 1);
      assertThat(results).containsExactlyInAnyOrder(uuid);
      firstListener.forEach(MessageListener::stopConsumer);
      results.clear();

      // register new consumer within the same consumer group
      bundle
            .registerMessageHandler(MessageHandlerRegistration // NOSONAR
                  .<String, String> builder()
                  .withDefaultListenerConfig()
                  .forTopic(topic)
                  .withDefaultConsumer()
                  .withKeyDeserializer(deserializer)
                  .withValueDeserializer(deserializer)
                  .withHandler(r -> results.add(r.value()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      // wait until consumer is up an running and read data
      await().atMost(N_MAX_WAIT_MS, MILLISECONDS);

      assertThat(results).withFailMessage("There must be no results read by second consumer").isEmpty();

   }

   @Test
   public void processingErrorsNotCommitedAndShouldBeRetried() {
      String topic = "processinErrorsNotCommitedAndShouldBeRetried";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

      AtomicInteger processingError = new AtomicInteger(0);
      List<Integer> testResults = Collections.synchronizedList(new ArrayList<Integer>());

      MessageHandler<String, Integer> handler = new MessageHandler<String, Integer>() {
         private int pollCount = 0;

         @Override
         public void handle(ConsumerRecord<String, Integer> record) {
            pollCount++;
            Integer value = record.value();
            if ((value % 2) == 0 && pollCount <= 4) {
               processingError.incrementAndGet();
               throw new ProcessingErrorRetryException("processing error of record: " + record.key());
            }

            testResults.add(value);
         }
      };

      ErrorHandler<String, Integer> errorHandler = (record, e, consumer) -> e instanceof ProcessingErrorRetryException;

      bundle
          .createMessageListener(MessageListenerRegistration
              .<String, Integer>builder()
              .withDefaultListenerConfig()
              .forTopic(topic)
              .withConsumerConfig(
                  ConsumerConfig.<String, Integer>builder().withGroup("test").addConfig("enable.auto.commit", "false")
                      .addConfig("max.poll.records", "5").build())
              .withValueDeserializer(new IntegerDeserializer())
              .withListenerStrategy( new RetryProcessingErrorMLS<>(handler, errorHandler))
              .build());

      KafkaProducer<String, Integer> producer = KAFKA
          .getKafkaTestUtils()
          .getKafkaProducer(StringSerializer.class, IntegerSerializer.class);
      IntStream.range(1, 21).forEach(e -> producer
          .send(new ProducerRecord<String, Integer>(topic, UUID.randomUUID().toString(), e)));

      await().atMost(4, SECONDS).until(() -> testResults.size() == 20);
      assertThat(processingError.get())
          .withFailMessage("There was at least 1 processing error")
          .isGreaterThanOrEqualTo(1);
      assertThat(testResults)
          .withFailMessage("There must be 20 results finally processed by consumer")
          .hasSize(20);
      assertThat(testResults).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
   }

   @Test
   public void processingErrorsNotCommitedAndShouldBeRetriedWith2Partitions() {
      String topic = "processingErrorsNotCommitedAndShouldBeRetriedWith2Partitions";
      KAFKA.getKafkaTestUtils().createTopic(topic, 2, (short) 1);

      AtomicInteger processingError = new AtomicInteger(0);
      List<Integer> testResults = Collections.synchronizedList(new ArrayList<Integer>());

      MessageHandler<String, Integer> handler = new MessageHandler<String, Integer>() {
         private int pollCount = 0;

         @Override
         public void handle(ConsumerRecord<String, Integer> record) {
            pollCount++;
            Integer value = record.value();
            if ((value % 2) == 0 && pollCount <= 4) {
               processingError.incrementAndGet();
               throw new ProcessingErrorRetryException("processing error of record: " + record.key());
            }

            testResults.add(value);
         }
      };

      ErrorHandler<String, Integer> errorHandler = (record, e, consumer) -> e instanceof ProcessingErrorRetryException;

      bundle
          .createMessageListener(MessageListenerRegistration
              .<String, Integer>builder()
              .withDefaultListenerConfig()
              .forTopicConfigs(Collections.singletonList(
                  new ExpectedTopicConfigurationBuilder(topic).withPartitionCount(2)
                      .withReplicationFactor(1).build()))
              .withConsumerConfig(
                  ConsumerConfig.<String, Integer>builder().withGroup("test").addConfig("enable.auto.commit", "false")
                      .addConfig("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                      .addConfig("value.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer")
                      .addConfig("max.poll.records", "5").build())
              //.withValueDeserializer(new IntegerDeserializer())
              .withListenerStrategy( new RetryProcessingErrorMLS<>(handler, errorHandler))
              .build());

      KafkaProducer<String, Integer> producer = KAFKA
          .getKafkaTestUtils()
          .getKafkaProducer(StringSerializer.class, IntegerSerializer.class);
      IntStream.range(1, 21).forEach(e -> producer
          .send(new ProducerRecord<String, Integer>(topic, UUID.randomUUID().toString(), e)));

      await().atMost(4, SECONDS).until(() -> testResults.size() == 20);
      assertThat(processingError.get())
          .withFailMessage("There was at least 1 processing error")
          .isGreaterThanOrEqualTo(1);
      assertThat(testResults)
          .withFailMessage("There must be 20 results finally processed by consumer")
          .hasSize(20);
      assertThat(testResults).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
   }
}
