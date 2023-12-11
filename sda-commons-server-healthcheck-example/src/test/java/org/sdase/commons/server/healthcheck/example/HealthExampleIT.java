package org.sdase.commons.server.healthcheck.example;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HealthExampleIT {

  private static final String SERVICE_PATH = "/service"; // NOSONAR
  private static final String HEALTH_CHECK = "healthcheck";
  private static final String HEALTH_INTERNAL = "healthcheck/internal";

  // Wiremock to mock external service
  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  // create example application
  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<HealthExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          HealthExampleApplication.class,
          null,
          randomPorts(),
          config("externalServiceUrl", () -> WIRE.url(SERVICE_PATH)));

  @BeforeEach
  void beforeEach() {
    WIRE.resetAll();
  }

  @AfterEach
  void afterEach() throws InterruptedException {
    HealthExampleApplication app = DW.getApplication();
    app.stopCountingThread();
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
    assertThat(getHealthStatus(HEALTH_CHECK)).isEqualTo(200);

    // stop counting thread and set application to unhealthy by doing so
    app.stopCountingThread();
    assertThat(getHealthStatus(HEALTH_CHECK)).isNotEqualTo(200);
  }

  @Test
  void internalHealthCheckStatusShouldBecomeUnhealthy() throws InterruptedException {
    mockHealthy();
    HealthExampleApplication app = DW.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealthStatus(HEALTH_INTERNAL)).isEqualTo(200);

    // stop counting thread and set application to unhealthy by doing so
    app.stopCountingThread();
    assertThat(getHealthStatus(HEALTH_INTERNAL)).isNotEqualTo(200);
  }

  @Test
  void externalHealthCheckDoesOnlyAffectOverallStatus() {
    mockUnhealthy();
    HealthExampleApplication app = DW.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealthStatus(HEALTH_CHECK)).isEqualTo(500);
    assertThat(getHealthStatus(HEALTH_INTERNAL)).isEqualTo(200);
  }

  private int getHealthStatus(String path) {
    try (var result =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path(path)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get()) {
      return result.getStatus();
    }
  }
}
