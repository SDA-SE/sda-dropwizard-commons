package org.sdase.commons.server.healthcheck.example;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class HealthExampleIT {

  private static final String SERVICE_PATH = "/service"; // NOSONAR
  private static final String HEALTH_CHECK = "healthcheck";
  private static final String HEALTH_INTERNAL = "healthcheck/internal";

  // Wiremock to mock external service
  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  // create example application
  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<HealthExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          HealthExampleApplication.class,
          null,
          randomPorts(),
          config("externalServiceUrl", () -> WIRE.url(SERVICE_PATH)));

  @BeforeEach
  void resetMock() {
    // WIRE.resetMappings();
    WIRE.resetAll();
  }

  private void mockHealthy() {
    WIRE.stubFor(get(SERVICE_PATH).willReturn(aResponse().withStatus(200)));
  }

  private void mockUnhealthy() {
    WIRE.stubFor(get(SERVICE_PATH).willReturn(aResponse().withStatus(500)));
  }

  @Test
  void overallHealthCheckStatusShouldBecomeUnhealthy() throws InterruptedException {
    mockHealthy();
    HealthExampleApplication app = DW.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealth(HEALTH_CHECK).getStatus()).isEqualTo(200);

    // stop counting thread and set application to unhealthy by doing so
    app.stopCountingThread();
    assertThat(getHealth(HEALTH_CHECK).getStatus()).isNotEqualTo(200);
  }

  @Test
  void internalHealthCheckStatusShouldBecomeUnhealthy() throws InterruptedException {
    mockHealthy();
    HealthExampleApplication app = DW.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealth(HEALTH_INTERNAL).getStatus()).isEqualTo(200);

    // stop counting thread and set application to unhealthy by doing so
    app.stopCountingThread();
    assertThat(getHealth(HEALTH_INTERNAL).getStatus()).isNotEqualTo(200);
  }

  @Test
  void externalHealthCheckDoesOnlyAffectOverallStatus() {
    mockUnhealthy();
    HealthExampleApplication app = DW.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealth(HEALTH_CHECK).getStatus()).isEqualTo(500);
    assertThat(getHealth(HEALTH_INTERNAL).getStatus()).isEqualTo(200);
  }

  private Response getHealth(String s) {
    return DW.client()
        .target("http://localhost:" + DW.getAdminPort())
        .path(s)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();
  }
}
