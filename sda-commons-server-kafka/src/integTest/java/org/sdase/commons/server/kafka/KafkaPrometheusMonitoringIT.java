/**
 * 
 */
package org.sdase.commons.server.kafka;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.iterable.Extractor;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.confluent.testing.KafkaBrokerEnvironmentRule;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.salesforce.kafka.test.junit4.SharedKafkaTestResource;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;

public class KafkaPrometheusMonitoringIT {

   private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPrometheusMonitoringIT.class);

   private static final SharedKafkaTestResource KAFKA = new SharedKafkaTestResource();

   private static final DropwizardAppRule<KafkaTestConfiguration> DROPWIZARD_APP_RULE = new DropwizardAppRule<>(
         KafkaTestApplication.class, ResourceHelpers.resourceFilePath("test-config-con-prod.yml"));

   private static final KafkaBrokerEnvironmentRule KAFKA_BROKER_ENVIRONMENT_RULE = new KafkaBrokerEnvironmentRule(
         KAFKA);

   @ClassRule
   public static final TestRule CHAIN = RuleChain.outerRule(KAFKA_BROKER_ENVIRONMENT_RULE).around(DROPWIZARD_APP_RULE);

   private List<String> resultsString = Collections.synchronizedList(new ArrayList<>());

   private KafkaBundle<KafkaTestConfiguration> kafkaBundle;

   @Before
   public void before() {
      KafkaTestApplication app = DROPWIZARD_APP_RULE.getApplication();
      // noinspection unchecked
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

      public static Extractor<MetricFamilySamples, String> name() {
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
