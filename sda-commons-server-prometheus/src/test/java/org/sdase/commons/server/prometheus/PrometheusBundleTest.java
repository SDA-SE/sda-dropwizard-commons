package org.sdase.commons.server.prometheus;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.prometheus.helper.MicrometerTestExtension;
import org.sdase.commons.server.prometheus.test.PrometheusTestApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MicrometerTestExtension.class)
class PrometheusBundleTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusBundleTest.class);

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(PrometheusTestApplication.class, null, randomPorts());

  private static final String REST_URI = "http://localhost:%d";
  private String resourceUri;

  @BeforeEach
  void beforeEach() {
    resourceUri = String.format(REST_URI, DW.getLocalPort());
  }

  @AfterEach
  void tearDown() {
    Metrics.globalRegistry.clear();
  }

  @Test
  void shouldTrackFiveRequests() {
    String metrics = readMetrics();
    String count = extractSpecificMetric(metrics, "http_request_duration_seconds_count");
    double oldCountValue = extractValue(count);

    // when
    for (int i = 0; i < 5; i++) {
      prepareResourceRequest().get(String.class);
    }

    // then
    metrics = readMetrics();
    count = extractSpecificMetric(metrics, "http_request_duration_seconds_count");

    double countValue = extractValue(count);
    assertThat(countValue).isEqualTo(oldCountValue + 5);

    // five requests summary
    assertThat(metrics).contains("http_request_duration_seconds_sum");
  }

  private String extractSpecificMetric(String metrics, String metricName) {
    return Stream.of(metrics.split("(\r\n|\r|\n)"))
        .filter(l -> l.contains(metricName))
        .findFirst()
        .orElse(null);
  }

  private double extractValue(String countMetric) {
    if (countMetric == null) {
      return 0;
    }
    return Double.parseDouble(countMetric.split(" ")[1]);
  }

  @Test
  void shouldReadConsumerNameFromRequestAttribute() {
    prepareResourceRequest().header("Consumer-Name", "test-consumer-from-name").get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("consumer_name=\"test-consumer-from-name\"");
  }

  @Test
  void shouldWriteHelpAndTypeToMetrics() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics)
        .contains("# HELP http_request_duration_seconds")
        .contains("# TYPE http_request_duration_seconds histogram");
  }

  @Test
  void shouldContainAllMetrics() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics)
        .contains(
            "consumer_name=\"\"",
            "method=\"GET\"",
            "implementing_method=\"pingResource\"",
            "resource_path=\"ping\"",
            "status_code=\"200\"",
            "io_dropwizard_");
  }

  @Test
  void shouldTrackResourcePathWithPathParam() {
    DW.client()
        .target(resourceUri)
        .path("path")
        .path("custom-path-param")
        .request()
        .get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("resource_path=\"path/{param}\"");
  }

  @Test
  void shouldMapCustomMetrics() {
    DW.client()
        .target(resourceUri)
        .path("client")
        .path("" + DW.getLocalPort())
        .request()
        .get(String.class);

    String metrics = readMetrics();

    assertThat(metrics)
        .contains(
            "apache_http_client_request_duration_seconds{manager=\"HttpClient\",method=\"get\",name=\"myClient\",quantile=\"0.5\",}");
  }

  @Test
  void shouldProvideHealthChecksAsPrometheusMetrics() {
    String healthChecks = readMetrics();

    assertThat(healthChecks)
        .contains("healthcheck_status{name=\"anUnhealthyCheck\",} 0.0")
        .contains("healthcheck_status{name=\"aHealthyCheck\",} 1.0");
  }

  @Test
  void shouldProvideHealthChecksAsPrometheusMetricsOnCustomEndpoint() {
    String healthChecks = readHealthChecks();

    assertThat(healthChecks)
        .contains("healthcheck_status{name=\"anUnhealthyCheck\",} 0.0")
        .contains("healthcheck_status{name=\"aHealthyCheck\",} 1.0");
  }

  @Test
  void shouldNotHttpCacheHealthCheck() {
    Response response =
        DW.client()
            .target(
                String.format("http://localhost:%d", DW.getAdminPort()) + "/healthcheck/prometheus")
            .request()
            .get();
    assertThat(response.getHeaders()).containsKey("Cache-Control");
    assertThat(response.getHeaders().getFirst("Cache-Control").toString())
        .contains("must-revalidate")
        .contains("no-cache")
        .contains("no-store");
  }

  @Test
  void micrometerMetricsAvailableInPrometheus() {

    MeterRegistry globalRegistry = Metrics.globalRegistry;
    Counter counter = globalRegistry.counter("micrometerTestCounter", "testTagKey", "testTagValue");

    counter.increment();
    counter.increment();

    ArrayList<Collector.MetricFamilySamples> testCounterTotal =
        Collections.list(
            CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(
                s -> s.equals("micrometerTestCounter_total")));

    assertThat(testCounterTotal).hasSize(1);

    Collector.MetricFamilySamples testCounterSamples = testCounterTotal.get(0);
    assertThat(testCounterSamples.name).isEqualTo("micrometerTestCounter");

    List<Collector.MetricFamilySamples.Sample> sampleList = testCounterSamples.samples;
    assertThat(sampleList).hasSize(1);

    assertThat(sampleList.get(0).labelNames).contains("testTagKey");
    assertThat(sampleList.get(0).labelValues).contains("testTagValue");

    assertThat(sampleList.get(0).value).isEqualTo(2);
  }

  //  Testing the same metric twice as an example of how to clear metrics.
  //  Please note the usage of Metrics.globalRegistry.clear(); in the tearDown method.
  @Test
  void micrometerMetricsAvailableInPrometheus2() {

    MeterRegistry globalRegistry = Metrics.globalRegistry;
    Counter counter = globalRegistry.counter("micrometerTestCounter", "testTagKey", "testTagValue");

    counter.increment();
    counter.increment();

    ArrayList<Collector.MetricFamilySamples> testCounterTotal =
        Collections.list(
            CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(
                s -> s.equals("micrometerTestCounter_total")));

    assertThat(testCounterTotal).hasSize(1);

    Collector.MetricFamilySamples testCounterSamples = testCounterTotal.get(0);
    assertThat(testCounterSamples.name).isEqualTo("micrometerTestCounter");

    List<Collector.MetricFamilySamples.Sample> sampleList = testCounterSamples.samples;
    assertThat(sampleList).hasSize(1);

    assertThat(sampleList.get(0).labelNames).contains("testTagKey");
    assertThat(sampleList.get(0).labelValues).contains("testTagValue");

    assertThat(sampleList.get(0).value).isEqualTo(2);
  }

  @Test
  void micrometerJVMMetricsAvailable() {

    MeterRegistry globalRegistry = Metrics.globalRegistry;

    List<Meter> meters = globalRegistry.getMeters();

    List<Meter> jvmBufferList =
        meters.stream()
            .filter(m -> m.getId().getName().startsWith("jvm.buffer"))
            .collect(Collectors.toList());
    List<Meter> jvmMemoryList =
        meters.stream()
            .filter(m -> m.getId().getName().startsWith("jvm.memory"))
            .collect(Collectors.toList());
    List<Meter> jvmProcessorList =
        meters.stream()
            .filter(m -> m.getId().getName().contains("cpu"))
            .collect(Collectors.toList());
    List<Meter> jvmThreadsList =
        meters.stream()
            .filter(m -> m.getId().getName().startsWith("jvm.threads"))
            .collect(Collectors.toList());
    List<Meter> jvmClassesList =
        meters.stream()
            .filter(m -> m.getId().getName().startsWith("jvm.classes"))
            .collect(Collectors.toList());
    List<Meter> jvmGcList =
        meters.stream()
            .filter(m -> m.getId().getName().startsWith("jvm.gc"))
            .collect(Collectors.toList());

    // assertions are not strict since metrics depend on Java version
    assertThat(jvmBufferList).hasSizeGreaterThanOrEqualTo(6);
    assertThat(jvmMemoryList).hasSizeGreaterThanOrEqualTo(24);
    assertThat(jvmProcessorList).hasSizeGreaterThanOrEqualTo(3);
    assertThat(jvmThreadsList).hasSizeGreaterThanOrEqualTo(10);
    assertThat(jvmClassesList).hasSizeGreaterThanOrEqualTo(2);
    assertThat(jvmGcList).hasSizeGreaterThanOrEqualTo(5);
  }

  private Invocation.Builder prepareResourceRequest() {
    return DW.client().target(resourceUri).path("ping").request();
  }

  private String readMetrics() {
    Response response =
        DW.client()
            .target(String.format("http://localhost:%d", DW.getAdminPort()) + "/metrics/prometheus")
            .request()
            .get();
    String metrics = response.readEntity(String.class);
    LOGGER.info("Prometheus metrics: {}", metrics);
    return metrics;
  }

  private String readHealthChecks() {
    Response response =
        DW.client()
            .target(
                String.format("http://localhost:%d", DW.getAdminPort()) + "/healthcheck/prometheus")
            .request()
            .get();
    String healthChecks = response.readEntity(String.class);
    LOGGER.info("Prometheus health checks: {}", healthChecks);
    return healthChecks;
  }
}
