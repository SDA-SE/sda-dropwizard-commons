package com.sdase.commons.server.prometheus;

import com.sdase.commons.server.prometheus.test.PrometheusTestApplication;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.stream.Stream;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusBundleTest {
   private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusBundleTest.class);

   // intentionally not a class rule, because we have to verify some parts of the metrics dependent of the test requests
   @Rule
   public final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(PrometheusTestApplication.class,
         resourceFilePath("test-config.yaml"));

   private static final String REST_URI = "http://localhost:%d";
   private String resourceUri;

   @Before
   public void setupClass() {
      resourceUri = String.format(REST_URI, DW.getLocalPort());
   }

   @Test
   public void shouldTrackFiveRequests() {
      for (int i = 0; i < 5; i++) {
         prepareResourceRequest().get(String.class);
      }

      String metrics = readMetrics();

      // five requests count provided
      assertThat(metrics).contains("http_request_duration_seconds_count");
      String count = Stream.of(metrics.split("(\r\n|\r|\n)"))
            .filter(l -> l.contains("http_request_duration_seconds_count"))
            .findFirst().orElse(null);
      assertThat(count).isNotBlank().startsWith("http_request_duration_seconds_count").endsWith("5.0");

      // five requests summary
      assertThat(metrics).contains("http_request_duration_seconds_sum");
   }

   @Test
   public void shouldReadConsumerNameFromRequestAttribute() {
      prepareResourceRequest().header("Consumer-Name", "test-consumer-from-name").get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("consumer_name=\"test-consumer-from-name\"");
   }

   @Test
   public void shouldWriteHelpAndTypeToMetrics() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics)
            .contains("# HELP http_request_duration_seconds")
            .contains("# TYPE http_request_duration_seconds histogram");
   }

   @Test
   public void shouldSkipConsumerName() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("consumer_name=\"\"");
   }

   @Test
   public void shouldTrackHttpMethod() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("method=\"GET\"");
   }

   @Test
   public void shouldTrackInvokedMethod() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("implementing_method=\"pingResource\"");
   }

   @Test
   public void shouldTrackResourcePath() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("resource_path=\"ping\"");
   }

   @Test
   public void shouldTrackResourcePathWithPathParam() {
      DW.client().target(resourceUri).path("path").path("custom-path-param").request().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("resource_path=\"path/{param}\"");
   }

   @Test
   public void shouldTrackStatusCode() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("status_code=\"200\"");
   }

   @Test
   public void shouldTrackDropwizardMetricsFromBridge() {
      prepareResourceRequest().get(String.class);

      String metrics = readMetrics();

      assertThat(metrics).contains("io_dropwizard_");
   }

   private Invocation.Builder prepareResourceRequest() {
      return DW.client().target(resourceUri).path("ping").request();
   }

   private String readMetrics() {
      Response response = DW
            .client()
            .target(String.format("http://localhost:%d", DW.getAdminPort()) + "/metrics/prometheus")
            .request()
            .get();
      String metrics = response.readEntity(String.class);
      LOGGER.info("Prometheus metrics: {}", metrics);
      return metrics;
   }

}
