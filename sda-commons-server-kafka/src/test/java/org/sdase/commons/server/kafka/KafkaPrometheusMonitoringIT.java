package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.strategies.autocommit.AutocommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.helper.MetricsHelper;
import org.sdase.commons.server.kafka.producer.MessageProducer;

class KafkaPrometheusMonitoringIT {

  @RegisterExtension
  @Order(0)
  static final SharedKafkaTestResource KAFKA =
      new SharedKafkaTestResource()
          // we only need one consumer offsets partition
          .withBrokerProperty("offsets.topic.num.partitions", "1")
          // we don't need to wait that a consumer group rebalances since we always start with a
          // fresh kafka instance
          .withBrokerProperty("group.initial.rebalance.delay.ms", "0");

  private static final String CONSUMER_1 = "consumer1";

  private static final String PRODUCER_1 = "producer1";

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<KafkaTestConfiguration> DROPWIZARD_APP_EXTENSION =
      new DropwizardAppExtension<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.brokers", KAFKA::getKafkaConnectString),

          // performance improvements in the tests
          config("kafka.config.heartbeat\\.interval\\.ms", "250"));

  private final List<Long> resultsLong = Collections.synchronizedList(new ArrayList<>());
  private final List<Long> resultsLong2 = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> kafkaBundle;

  @BeforeEach
  void before() {
    KafkaTestApplication app = DROPWIZARD_APP_EXTENSION.getApplication();

    kafkaBundle = app.kafkaBundle();

    resultsLong.clear();
    resultsLong2.clear();
  }

  @Test
  void writeKafkaInternalMetricsToPrometheus() {
    String topic = "writeKafkaInternalMetricsToPrometheus_Topic_1";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    String topic2 = "writeKafkaInternalMetricsToPrometheus_Topic_2";
    KAFKA.getKafkaTestUtils().createTopic(topic2, 1, (short) 1);

    AutocommitMLS<Long, Long> longLongAutocommitMLS =
        new AutocommitMLS<>(
            consumerRecord -> resultsLong.add(consumerRecord.value()),
            new IgnoreAndProceedErrorHandler<>());
    createMessageListener(topic, CONSUMER_1, longLongAutocommitMLS);

    AutocommitMLS<Long, Long> longLongAutocommitMLS2 =
        new AutocommitMLS<>(
            consumerRecord -> resultsLong2.add(consumerRecord.value()),
            new IgnoreAndProceedErrorHandler<>());
    createMessageListener(topic2, "consumer3", longLongAutocommitMLS2);

    MessageProducer<Long, Long> producer = registerProducer(topic, PRODUCER_1);
    MessageProducer<Long, Long> producer2 = registerProducer(topic2, "producer3");

    // pass in messages
    producer.send(1L, 1L);
    producer.send(2L, 2L);

    producer2.send(1L, 1L);
    producer2.send(2L, 2L);

    attachPrometheusToMicrometer();

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsLong.size() == 2);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsLong2.size() == 2);

    MetricSnapshots snapshots = PrometheusRegistry.defaultRegistry.scrape();

    MetricSnapshot metricSnapshot =
        snapshots.stream()
            .filter(f -> f.getMetadata().getName().equals("kafka_producer_record_error"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Metric kafka_producer_record_error not found"));

    assertThat(metricSnapshot).isNotNull();

    assertThat(metricSnapshot.getDataPoints()).hasSize(2);

    List<String> labels =
        metricSnapshot.getDataPoints().stream()
            .flatMap(dataPointSnapshot -> dataPointSnapshot.getLabels().stream())
            .map(Label::getValue)
            .toList();

    assertThat(labels).contains("producer1", "producer3");
  }

  @Test
  void checkIfInternalMetricChange() {
    String topic = "checkIfInternalMetricChange_Topic";
    KAFKA.getKafkaTestUtils().createTopic(topic, 1, (short) 1);

    MessageProducer<Long, Long> producer = registerProducer(topic, PRODUCER_1);

    AutocommitMLS<Long, Long> longLongAutocommitMLS =
        new AutocommitMLS<>(
            consumerRecord -> resultsLong.add(consumerRecord.value()),
            new IgnoreAndProceedErrorHandler<>());
    createMessageListener(topic, CONSUMER_1, longLongAutocommitMLS);

    producer.send(1L, 1L);
    producer.send(2L, 2L);

    await()
        .atMost(KafkaBundleConsts.N_MAX_WAIT_MS, MILLISECONDS)
        .until(() -> resultsLong.size() == 2);

    assertThat(MetricsHelper.getListOfMetrics()).containsAll(getProducerMetricsThatShouldExist());
    assertThat(MetricsHelper.getListOfMetrics()).containsAll(getConsumerMetricsThatShouldExist());
  }

  //  Keep in mind that internal metrics use minus while prometheus uses underscore
  private List<String> getProducerMetricsThatShouldExist() {

    return List.of(
        "producer-metrics-flush-time-ns-total",
        "producer-metrics-txn-init-time-ns-total",
        "producer-metrics-txn-begin-time-ns-total",
        "producer-metrics-txn-send-offsets-time-ns-total",
        "producer-metrics-txn-commit-time-ns-total",
        "producer-metrics-txn-abort-time-ns-total",
        "producer-metrics-metadata-wait-time-ns-total",
        "producer-metrics-buffer-exhausted-total",
        "producer-metrics-buffer-exhausted-rate",
        "producer-metrics-bufferpool-wait-time-total",
        "producer-metrics-bufferpool-wait-ratio",
        "producer-metrics-bufferpool-wait-time-ns-total",
        "producer-metrics-waiting-threads",
        "producer-metrics-buffer-total-bytes",
        "producer-metrics-buffer-available-bytes",
        "producer-metrics-produce-throttle-time-avg",
        "producer-metrics-produce-throttle-time-max",
        "producer-metrics-connection-close-total",
        "producer-metrics-connection-close-rate",
        "producer-metrics-connection-creation-total",
        "producer-metrics-connection-creation-rate",
        "producer-metrics-successful-authentication-total",
        "producer-metrics-successful-authentication-rate",
        "producer-metrics-successful-reauthentication-total",
        "producer-metrics-successful-reauthentication-rate",
        "producer-metrics-successful-authentication-no-reauth-total",
        "producer-metrics-failed-authentication-total",
        "producer-metrics-failed-authentication-rate",
        "producer-metrics-failed-reauthentication-total",
        "producer-metrics-failed-reauthentication-rate",
        "producer-metrics-reauthentication-latency-max",
        "producer-metrics-reauthentication-latency-avg",
        "producer-metrics-network-io-total",
        "producer-metrics-network-io-rate",
        "producer-metrics-outgoing-byte-total",
        "producer-metrics-outgoing-byte-rate",
        "producer-metrics-request-total",
        "producer-metrics-request-rate",
        "producer-metrics-request-size-avg",
        "producer-metrics-request-size-max",
        "producer-metrics-incoming-byte-total",
        "producer-metrics-incoming-byte-rate",
        "producer-metrics-response-total",
        "producer-metrics-response-rate",
        "producer-metrics-select-total",
        "producer-metrics-select-rate",
        "producer-metrics-io-wait-time-ns-avg",
        "producer-metrics-io-waittime-total",
        "producer-metrics-io-wait-ratio",
        "producer-metrics-io-wait-time-ns-total",
        "producer-metrics-io-time-ns-avg",
        "producer-metrics-iotime-total",
        "producer-metrics-io-ratio",
        "producer-metrics-io-time-ns-total",
        "producer-metrics-connection-count",
        "producer-metrics-batch-size-avg",
        "producer-metrics-batch-size-max",
        "producer-metrics-compression-rate-avg",
        "producer-metrics-record-queue-time-avg",
        "producer-metrics-record-queue-time-max",
        "producer-metrics-request-latency-avg",
        "producer-metrics-request-latency-max",
        "producer-metrics-record-send-total",
        "producer-metrics-record-send-rate",
        "producer-metrics-records-per-request-avg",
        "producer-metrics-record-retry-total",
        "producer-metrics-record-retry-rate",
        "producer-metrics-record-error-total",
        "producer-metrics-record-error-rate",
        "producer-metrics-record-size-max",
        "producer-metrics-record-size-avg",
        "producer-metrics-requests-in-flight",
        "producer-metrics-metadata-age",
        "producer-metrics-batch-split-total",
        "producer-metrics-batch-split-rate",
        "app-info-version",
        "app-info-commit-id",
        "app-info-start-time-ms",
        "producer-node-metrics-request-total",
        "producer-node-metrics-request-rate",
        "producer-node-metrics-request-size-avg",
        "producer-node-metrics-request-size-max",
        "producer-node-metrics-outgoing-byte-total",
        "producer-node-metrics-outgoing-byte-rate",
        "producer-node-metrics-response-total",
        "producer-node-metrics-response-rate",
        "producer-node-metrics-incoming-byte-total",
        "producer-node-metrics-incoming-byte-rate",
        "producer-node-metrics-request-latency-avg",
        "producer-node-metrics-request-latency-max",
        "producer-topic-metrics-record-send-total",
        "producer-topic-metrics-record-send-total",
        "producer-topic-metrics-record-send-rate",
        "producer-topic-metrics-byte-total",
        "producer-topic-metrics-byte-rate",
        "producer-topic-metrics-compression-rate",
        "producer-topic-metrics-record-retry-total",
        "producer-topic-metrics-record-retry-rate",
        "producer-topic-metrics-record-error-total",
        "producer-topic-metrics-record-error-rate");
  }

  private List<String> getConsumerMetricsThatShouldExist() {

    return List.of(
        "consumer-fetch-manager-metrics-fetch-throttle-time-avg",
        "consumer-fetch-manager-metrics-fetch-throttle-time-max",
        "consumer-fetch-manager-metrics-fetch-size-avg",
        "consumer-fetch-manager-metrics-fetch-size-max",
        "consumer-fetch-manager-metrics-bytes-consumed-total",
        "consumer-fetch-manager-metrics-bytes-consumed-rate",
        "consumer-fetch-manager-metrics-records-per-request-avg",
        "consumer-fetch-manager-metrics-records-consumed-total",
        "consumer-fetch-manager-metrics-records-consumed-rate",
        "consumer-fetch-manager-metrics-fetch-latency-avg",
        "consumer-fetch-manager-metrics-fetch-latency-max",
        "consumer-fetch-manager-metrics-fetch-total",
        "consumer-fetch-manager-metrics-fetch-rate",
        "consumer-fetch-manager-metrics-records-lag-max",
        "consumer-fetch-manager-metrics-records-lead-min",
        "consumer-metrics-connection-close-total",
        "consumer-metrics-connection-close-rate",
        "consumer-metrics-connection-creation-rate",
        "consumer-metrics-successful-authentication-total",
        "consumer-metrics-successful-authentication-rate",
        "consumer-metrics-connection-creation-total",
        "consumer-metrics-successful-reauthentication-total",
        "consumer-metrics-successful-reauthentication-rate",
        "consumer-metrics-successful-authentication-no-reauth-total",
        "consumer-metrics-failed-authentication-total",
        "consumer-metrics-failed-authentication-rate",
        "consumer-metrics-failed-reauthentication-total",
        "consumer-metrics-failed-reauthentication-rate",
        "consumer-metrics-reauthentication-latency-max",
        "consumer-metrics-reauthentication-latency-avg",
        "consumer-metrics-network-io-total",
        "consumer-metrics-network-io-rate",
        "consumer-metrics-outgoing-byte-total",
        "consumer-metrics-outgoing-byte-rate",
        "consumer-metrics-request-total",
        "consumer-metrics-request-rate",
        "consumer-metrics-request-size-avg",
        "consumer-metrics-request-size-max",
        "consumer-metrics-incoming-byte-total",
        "consumer-metrics-incoming-byte-rate",
        "consumer-metrics-response-total",
        "consumer-metrics-response-rate",
        "consumer-metrics-select-total",
        "consumer-metrics-select-rate",
        "consumer-metrics-io-wait-time-ns-avg",
        "consumer-metrics-io-waittime-total",
        "consumer-metrics-io-wait-ratio",
        "consumer-metrics-io-wait-time-ns-total",
        "consumer-metrics-io-time-ns-avg",
        "consumer-metrics-iotime-total",
        "consumer-metrics-io-ratio",
        "consumer-metrics-io-time-ns-total",
        "consumer-metrics-connection-count",
        "consumer-coordinator-metrics-heartbeat-response-time-max",
        "consumer-coordinator-metrics-heartbeat-total",
        "consumer-coordinator-metrics-heartbeat-rate",
        "consumer-coordinator-metrics-join-time-avg",
        "consumer-coordinator-metrics-join-time-max",
        "consumer-coordinator-metrics-join-total",
        "consumer-coordinator-metrics-join-rate",
        "consumer-coordinator-metrics-sync-time-avg",
        "consumer-coordinator-metrics-sync-time-max",
        "consumer-coordinator-metrics-sync-total",
        "consumer-coordinator-metrics-sync-rate",
        "consumer-coordinator-metrics-rebalance-latency-avg",
        "consumer-coordinator-metrics-rebalance-latency-max",
        "consumer-coordinator-metrics-rebalance-latency-total",
        "consumer-coordinator-metrics-rebalance-total",
        "consumer-coordinator-metrics-rebalance-rate-per-hour",
        "consumer-coordinator-metrics-failed-rebalance-total",
        "consumer-coordinator-metrics-failed-rebalance-rate-per-hour",
        "consumer-coordinator-metrics-last-rebalance-seconds-ago",
        "consumer-coordinator-metrics-last-heartbeat-seconds-ago",
        "consumer-coordinator-metrics-commit-latency-avg",
        "consumer-coordinator-metrics-commit-latency-max",
        "consumer-coordinator-metrics-commit-total",
        "consumer-coordinator-metrics-commit-rate",
        "consumer-coordinator-metrics-partition-revoked-latency-avg",
        "consumer-coordinator-metrics-partition-revoked-latency-max",
        "consumer-coordinator-metrics-partition-assigned-latency-avg",
        "consumer-coordinator-metrics-partition-assigned-latency-max",
        "consumer-coordinator-metrics-partition-lost-latency-avg",
        "consumer-coordinator-metrics-partition-lost-latency-max",
        "consumer-coordinator-metrics-assigned-partitions",
        "consumer-metrics-last-poll-seconds-ago",
        "consumer-metrics-time-between-poll-avg",
        "consumer-metrics-time-between-poll-max",
        "consumer-metrics-poll-idle-ratio-avg",
        "consumer-metrics-commit-sync-time-ns-total",
        "consumer-metrics-committed-time-ns-total",
        "consumer-node-metrics-request-total",
        "consumer-node-metrics-request-rate",
        "consumer-node-metrics-request-size-avg",
        "consumer-node-metrics-request-size-max",
        "consumer-node-metrics-outgoing-byte-total",
        "consumer-node-metrics-outgoing-byte-rate",
        "consumer-node-metrics-response-total",
        "consumer-node-metrics-response-rate",
        "consumer-node-metrics-incoming-byte-total",
        "consumer-node-metrics-incoming-byte-rate",
        "consumer-node-metrics-request-latency-avg",
        "consumer-node-metrics-request-latency-max",
        "consumer-fetch-manager-metrics-preferred-read-replica",
        "consumer-fetch-manager-metrics-records-lag",
        "consumer-fetch-manager-metrics-records-lag-avg",
        "consumer-fetch-manager-metrics-records-lead",
        "consumer-fetch-manager-metrics-records-lead-avg");
  }

  private void createMessageListener(
      String topic, String consumerConfigKey, AutocommitMLS<Long, Long> longLongAutocommitMLS) {
    kafkaBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(topic)
            .withConsumerConfig(consumerConfigKey)
            .withListenerStrategy(longLongAutocommitMLS)
            .build());
  }

  private MessageProducer<Long, Long> registerProducer(String topic, String producerConfigKey) {
    return kafkaBundle.registerProducer(
        ProducerRegistration.<Long, Long>builder()
            .forTopic(topic)
            .withProducerConfig(producerConfigKey)
            .build());
  }

  private void attachPrometheusToMicrometer() {
    PrometheusMeterRegistry meterRegistry =
        new PrometheusMeterRegistry(key -> null, PrometheusRegistry.defaultRegistry, Clock.SYSTEM);
    Metrics.addRegistry(meterRegistry);
  }
}
