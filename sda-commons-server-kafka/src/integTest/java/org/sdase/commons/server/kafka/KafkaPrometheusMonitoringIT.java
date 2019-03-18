package org.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.salesforce.kafka.test.KafkaBroker;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.config.ConsumerConfig;
import org.sdase.commons.server.kafka.config.ProducerConfig;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;

import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;

public class KafkaPrometheusMonitoringIT {

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource();

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

                  kafka
                        .getConsumers()
                        .put("consumer1", ConsumerConfig
                              .builder()
                              .withGroup("default")
                              .addConfig("key.deserializer", "org.apache.kafka.common.serialization.LongDeserializer")
                              .addConfig("value.deserializer", "org.apache.kafka.common.serialization.LongDeserializer")
                              .build());

                  kafka
                        .getProducers()
                        .put("producer1",
                              ProducerConfig
                                    .builder()
                                    .addConfig("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
                                    .addConfig("value.serializer", "org.apache.kafka.common.serialization.LongSerializer")
                                    .build());
                  kafka.getProducers().put("producer2", ProducerConfig.builder().build());
               })
         .build()
   );

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA).around(DROPWIZARD_APP_RULE);

   private List<String> resultsString = Collections.synchronizedList(new ArrayList<>());

   private KafkaBundle<KafkaTestConfiguration> kafkaBundle;

   @Before
   public void before() {
      KafkaTestApplication app = DROPWIZARD_APP_RULE.getRule().getApplication();
      kafkaBundle = app.kafkaBundle();
      resultsString.clear();
   }

   @Test
   public void shouldWriteHelpAndTypeToMetrics() {
      String topic = "testConsumeMsgForPrometheus";
      KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

      kafkaBundle.registerMessageHandler(MessageHandlerRegistration
            .<String, String> builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig("consumer1")
            .withHandler(record -> resultsString.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());

      MessageProducer<Long, Long> producer = kafkaBundle.registerProducer(
            ProducerRegistration.<Long, Long> builder().forTopic(topic).withProducerConfig("producer1").build());

      // pass in messages
      producer.send(1L, 1L);
      producer.send(2L, 2L);

      await().atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS).until(() -> resultsString.size() == 2);

      List<MetricFamilySamples> list = Collections.list(CollectorRegistry.defaultRegistry.metricFamilySamples());

      String[] metrics = { "kafka_producer_topic_message_total", "kafka_consumer_topic_message_duration",
            "kafka_consumer_records_lag" };

      assertThat(list).extracting(MetricExtractor.name()).contains(metrics);

      list.forEach(mfs -> assertThat(mfs.samples.size()).isGreaterThan(0));

      assertThat(CollectorRegistry.defaultRegistry.getSampleValue("kafka_producer_topic_message_total",
            new String[] { "producer_name", "topic_name" }, new String[] { "producer-1", topic }))
                  .as("sample value for metric 'kafka_producer_topic_message_total'")
                  .isEqualTo(2);

      assertThat(CollectorRegistry.defaultRegistry.getSampleValue("kafka_consumer_topic_message_duration_count",
            new String[] { "consumer_name", "topic_name" }, new String[] { "consumer-1", topic }))
                  .as("sample value for metric 'kafka_consumer_topic_message_duration_count'")
                  .isEqualTo(2);
   }

   static class MetricExtractor implements Extractor<MetricFamilySamples, String> {

      private MetricExtractor() {
      }

      static Extractor<MetricFamilySamples, String> name() {
         return new MetricExtractor();
      }

      /**
       * @see org.assertj.core.api.iterable.Extractor#extract(java.lang.Object)
       */
      @Override
      public String extract(MetricFamilySamples input) {
         return input.name;
      }
   }
}
