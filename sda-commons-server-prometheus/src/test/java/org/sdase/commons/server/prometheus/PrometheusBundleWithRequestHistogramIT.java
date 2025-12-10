package org.sdase.commons.server.prometheus;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.Response;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.prometheus.helper.MicrometerTestExtension;
import org.sdase.commons.server.prometheus.test.PrometheusConfiguredTestApplication;
import org.sdase.commons.server.prometheus.test.PrometheusTestConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MicrometerTestExtension.class)
class PrometheusBundleWithRequestHistogramIT {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PrometheusBundleWithRequestHistogramIT.class);

  @RegisterExtension
  static final DropwizardAppExtension<PrometheusTestConfiguration> DW =
      new DropwizardAppExtension<>(
          PrometheusConfiguredTestApplication.class,
          resourceFilePath("prometheus-test-config-histogram.yaml"),
          randomPorts());

  private static final String REST_URI = "http://localhost:%d";
  private String resourceUri;

  @BeforeEach
  void beforeEach() {
    resourceUri = String.format(REST_URI, DW.getLocalPort());
  }

  @Test
  void shouldTrackRequestsAndAddHistogram() {
    // check for
    // http_server_requests_seconds_bucket{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/path/{param}",le="0.01",} 1.0
    // http_server_requests_seconds_bucket{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/path/{param}",le="0.4",} 1.0
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
    String bucket01 =
        extractSpecificMetric(metrics, "http_server_requests_seconds_bucket", "le=\"0.01\"");
    assertThat(bucket01).isNotNull();
    double bucketValue01 = extractValue(bucket01);
    assertThat(bucketValue01).isEqualTo(0.0);
    String bucket04 =
        extractSpecificMetric(metrics, "http_server_requests_seconds_bucket", "le=\"0.4\"");
    assertThat(bucket04).isNotNull();
    double bucketValue04 = extractValue(bucket04);
    assertThat(bucketValue04).isEqualTo(1.0);
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
