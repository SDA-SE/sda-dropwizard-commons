package org.sdase.commons.server.trace;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.trace.test.TraceTokenTestApp;

class TraceTokenBundleTest {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          TraceTokenTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldReadTraceToken() {
    String token =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Trace-Token", "test-trace-token")
            .get(String.class);
    assertThat(token).isEqualTo("test-trace-token");
  }

  @Test
  void shouldRespondWithGivenTraceToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Trace-Token", "test-trace-token")
            .get();
    assertThat(response.getHeaderString("Trace-Token")).isEqualTo("test-trace-token");
  }

  @Test
  void shouldGenerateTraceToken() {
    String token =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .get(String.class);
    assertThat(token).isNotBlank();
  }

  @Test
  void shouldAddTokenToResponse() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .get();

    String header = response.getHeaderString("Trace-Token");
    String property = response.readEntity(String.class);

    assertThat(header).isEqualTo(property);
  }

  @Test
  void shouldDoNothingOnOptions() {
    Response response =
        DW.client().target("http://localhost:" + DW.getLocalPort()).request().options();
    String header = response.getHeaderString("Trace-Token");
    assertThat(header).isBlank();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
