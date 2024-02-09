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
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
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

  @Test
  void shouldTrackFiveRequests() {
    String metrics = readMetrics();
    String count =
        extractSpecificMetric(metrics, "http_server_requests_seconds_count", "uri=\"/ping\"");
    double oldCountValue = extractValue(count);

    // when
    for (int i = 0; i < 5; i++) {
      prepareResourceRequest().get(String.class);
    }

    // then
    metrics = readMetrics();
    count = extractSpecificMetric(metrics, "http_server_requests_seconds_count", "uri=\"/ping\"");

    double countValue = extractValue(count);
    assertThat(countValue).isEqualTo(oldCountValue + 5);

    // five requests summary
    assertThat(metrics).contains("http_server_requests_seconds_count");
  }

  @Test
  void shouldTrackRequestsWithMicrometer() {
    // check for
    // http_server_requests_seconds_count{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/path/{param}",} 1.0
    // http_server_requests_seconds_sum{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/path/{param}",} 8.42042E-4
    // http_server_requests_seconds_max{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/path/{param}",} 8.42042E-4

    DW.client().target(resourceUri).path("path").path("some-value").request().get(String.class);

    // then
    var metrics = readMetrics();
    String counter =
        extractSpecificMetric(
            metrics, "http_server_requests_seconds_count", "uri=\"/path/{param}\"");
    assertThat(counter).isNotNull();
    double countValue = extractValue(counter);
    assertThat(countValue).isEqualTo(1.0d);
    String sum =
        extractSpecificMetric(metrics, "http_server_requests_seconds_sum", "uri=\"/path/{param}\"");
    double sumValue = extractValue(sum);
    assertThat(sumValue).isPositive();
    String max =
        extractSpecificMetric(metrics, "http_server_requests_seconds_max", "uri=\"/path/{param}\"");
    double maxValue = extractValue(max);
    assertThat(maxValue).isPositive().isEqualTo(sumValue);
  }

  private String extractSpecificMetric(String metrics, String metricName, String tagMatch) {
    return Stream.of(metrics.split("(\r\n|\r|\n)"))
        .filter(l -> l.contains(metricName))
        .filter(l -> tagMatch == null || l.contains(tagMatch))
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
  void shouldContainAllTags() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();
    // check all tags available
    // http_server_requests_seconds_count{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/ping",} 1.0
    assertThat(metrics)
        .contains(
            "exception=\"None\"",
            "method=\"GET\"",
            "outcome=\"SUCCESS\"",
            "status=\"200\"",
            "uri=\"/ping\"");
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

    //
    // http_server_requests_seconds_count{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/path/{param}",} 1.0
    assertThat(metrics).contains("uri=\"/path/{param}\"");
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
  void shouldNotHttpCacheHealthCheck() {
    try (Response response =
        DW.client()
            .target(
                String.format("http://localhost:%d", DW.getAdminPort()) + "/healthcheck/prometheus")
            .request()
            .get()) {
      assertThat(response.getHeaders()).containsKey("Cache-Control");
      assertThat(response.getHeaders().getFirst("Cache-Control").toString())
          .contains("must-revalidate")
          .contains("no-cache")
          .contains("no-store");
    }
  }

  @Test
  void micrometerMetricsAvailableInPrometheus1() {
    assertMicrometerMetricsInPrometheus();
  }

  //  Testing the same metric twice as an example of how to clear metrics.
  //  Please note the removal in finally.
  @Test
  void micrometerMetricsAvailableInPrometheus2() {
    assertMicrometerMetricsInPrometheus();
  }

  private void assertMicrometerMetricsInPrometheus() {
    MeterRegistry globalRegistry = Metrics.globalRegistry;
    Counter counter = globalRegistry.counter("micrometerTestCounter", "testTagKey", "testTagValue");

    try {

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
    } finally {
      Metrics.globalRegistry.remove(counter);
    }
  }

  @Test
  void micrometerJVMMetricsAvailable() {

    MeterRegistry globalRegistry = Metrics.globalRegistry;

    List<Meter> meters = globalRegistry.getMeters();

    List<Meter> jvmBufferList =
        meters.stream().filter(m -> m.getId().getName().startsWith("jvm.buffer")).toList();
    List<Meter> jvmMemoryList =
        meters.stream().filter(m -> m.getId().getName().startsWith("jvm.memory")).toList();
    List<Meter> jvmProcessorList =
        meters.stream().filter(m -> m.getId().getName().contains("cpu")).toList();
    List<Meter> jvmThreadsList =
        meters.stream().filter(m -> m.getId().getName().startsWith("jvm.threads")).toList();
    List<Meter> jvmClassesList =
        meters.stream().filter(m -> m.getId().getName().startsWith("jvm.classes")).toList();
    List<Meter> jvmGcList =
        meters.stream().filter(m -> m.getId().getName().startsWith("jvm.gc")).toList();

    // assertions are not strict since metrics depend on Java version
    assertThat(jvmBufferList).hasSizeGreaterThanOrEqualTo(6);
    assertThat(jvmMemoryList).hasSizeGreaterThanOrEqualTo(24);
    assertThat(jvmProcessorList).hasSizeGreaterThanOrEqualTo(3);
    assertThat(jvmThreadsList).hasSizeGreaterThanOrEqualTo(10);
    assertThat(jvmClassesList).hasSizeGreaterThanOrEqualTo(2);
    assertThat(jvmGcList).hasSizeGreaterThanOrEqualTo(5);
  }

  @Test
  void micrometerJettyMetricsAvailable() {

    MeterRegistry globalRegistry = Metrics.globalRegistry;
    List<Meter> meters = globalRegistry.getMeters();
    List<Meter> jettyList =
        meters.stream().filter(m -> m.getId().getName().startsWith("jetty")).toList();

    // assertions are not strict since metrics depend on Java version
    assertThat(jettyList).hasSizeGreaterThanOrEqualTo(12);
  }

  private Invocation.Builder prepareResourceRequest() {
    return DW.client().target(resourceUri).path("ping").request();
  }

  private String readMetrics() {
    try (Response response =
        DW.client()
            .target(String.format("http://localhost:%d", DW.getAdminPort()) + "/metrics/prometheus")
            .request()
            .get()) {
      var metrics = response.readEntity(String.class);
      LOGGER.info("Prometheus metrics: {}", metrics);
      return metrics;
    }
  }
}
