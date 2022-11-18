package org.sdase.commons.server.prometheus;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.stream.Stream;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.prometheus.test.PrometheusTestApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PrometheusBundleTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusBundleTest.class);

  @RegisterExtension
  private static final DropwizardAppExtension<Configuration> DW =
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
  void shouldSkipConsumerName() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("consumer_name=\"\"");
  }

  @Test
  void shouldTrackHttpMethod() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("method=\"GET\"");
  }

  @Test
  void shouldTrackInvokedMethod() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("implementing_method=\"pingResource\"");
  }

  @Test
  void shouldTrackResourcePath() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("resource_path=\"ping\"");
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
  void shouldTrackStatusCode() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("status_code=\"200\"");
  }

  @Test
  void shouldTrackDropwizardMetricsFromBridge() {
    prepareResourceRequest().get(String.class);

    String metrics = readMetrics();

    assertThat(metrics).contains("io_dropwizard_");
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
