package org.sdase.commons.server.starter;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;
import static org.sdase.commons.server.testing.DropwizardRuleHelper.dropwizardTestAppFrom;

import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.starter.test.StarterApp;

public class FilterPriorityTest {

  @ClassRule
  public static final DropwizardAppRule<SdaPlatformConfiguration> DW =
      dropwizardTestAppFrom(StarterApp.class)
          .withConfigFrom(SdaPlatformConfiguration::new)
          .withRandomPorts()
          .withRootPath("/api/*")
          .build();

  @Test
  public void corsFromSwaggerHasHigherPriority() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/swagger.yaml")
            .request("application/yaml")
            .header("Origin", "example.com")
            .get();

    assertThat(response.getStatus()).isEqualTo(OK_200);
    assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
  }

  @Test
  public void traceTokenFilterHasHighestPriority() {
    // Make sure that trace token filter is even executed when authentication fails
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .header("Trace-Token", "MyTraceToken")
            .get();

    assertThat(response.getHeaderString("Trace-Token")).isEqualTo("MyTraceToken");
  }

  @Test
  public void consumerTokenValidationHappensBeforeAuthentication() {
    // Make sure that the consumer token filter fails before the authentication filter
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.readEntity(String.class)).contains("Consumer token is required");
  }

  @Test
  public void errorsInConsumerTokenFilterTrackedByPrometheus() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(401);

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
  public void errorsInAuthenticationFilterAreTrackedByPrometheus() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .header("Consumer-Token", "MyConsumer")
            .get();

    assertThat(response.getStatus()).isEqualTo(401);

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
