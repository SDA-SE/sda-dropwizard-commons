package org.sdase.commons.starter;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.opa.testing.OpaClassExtension;
import org.sdase.commons.starter.test.StarterApp;

class FilterPriorityTest {

  @RegisterExtension
  @Order(0)
  public static final OpaClassExtension OPA = new OpaClassExtension();

  @RegisterExtension
  @Order(1)
  public static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          StarterApp.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  @Test
  void corsFromSwaggerHasHigherPriority() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/openapi.yaml")
            .request("application/yaml")
            .header("Origin", "example.com")
            .get();

    assertThat(response.getStatus()).isEqualTo(OK_200);
    assertThat(response.getMetadata().getFirst("Access-Control-Allow-Origin"))
        .isEqualTo("example.com");
  }

  @Test
  void traceTokenFilterHasHighestPriority() {
    // Make sure that trace token filter is even executed when authentication fails
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .header("Trace-Token", "MyTraceToken")
            .get();

    assertThat(response.getMetadata().getFirst("Trace-Token")).isEqualTo("MyTraceToken");
  }

  @Test
  void errorsInConsumerTokenFilterTrackedByPrometheus() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(403);

    String metrics =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("metrics")
            .path("prometheus")
            .request(APPLICATION_JSON)
            .get(String.class);

    assertThat(metrics).contains("consumer_name=\"\"");
  }

  @Test
  void errorsInAuthenticationFilterAreTrackedByPrometheus() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "MyConsumer")
            .get();

    assertThat(response.getStatus()).isEqualTo(403);

    String metrics =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("metrics")
            .path("prometheus")
            .request(APPLICATION_JSON)
            .get(String.class);

    assertThat(metrics).contains("consumer_name=\"MyConsumer\"");
  }
}
