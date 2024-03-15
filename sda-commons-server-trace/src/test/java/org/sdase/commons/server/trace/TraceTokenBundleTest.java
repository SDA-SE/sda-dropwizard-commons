package org.sdase.commons.server.trace;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.trace.test.TraceTokenTestApp;

class TraceTokenBundleTest {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          TraceTokenTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"),
          ConfigOverride.config(
              "logging.appenders[0].logFormat", "%logger{0} %message %mdc{Trace-Token:-}"));

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
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .header("Trace-Token", "test-trace-token")
            .get()) {
      assertThat(response.getHeaderString("Trace-Token")).isEqualTo("test-trace-token");
    }
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
    String header;
    String property;
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/api/token")
            .request(APPLICATION_JSON)
            .get()) {

      header = response.getHeaderString("Trace-Token");
      property = response.readEntity(String.class);
    }

    assertThat(header).isEqualTo(property);
  }

  @Test
  void shouldDoNothingOnOptions() {
    try (Response response =
        DW.client().target("http://localhost:" + DW.getLocalPort()).request().options()) {
      String header = response.getHeaderString("Trace-Token");
      assertThat(header).isBlank();
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  @StdIo
  @SuppressWarnings("JUnitMalformedDeclaration")
  void shouldHaveTraceTokenRequestedInTask(StdOut out) {
    String expectedLogPattern =
        "^TraceTokenAwareExampleTask Log with a TraceToken in the MDC. (.+)$";
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("tasks")
            .path("example-task")
            .request()
            .post(Entity.text("Test"))) {

      assertThat(response.getStatus()).isEqualTo(200);

      var traceToken =
          Arrays.stream(out.capturedLines())
              .filter(l -> l.matches(expectedLogPattern))
              .map(l -> l.replaceAll(expectedLogPattern, "$1"))
              .findFirst()
              .orElse(null);

      assertThat(response.readEntity(String.class).trim())
          .isEqualTo("Trace-Token: %s".formatted(traceToken));
    }
  }
}
