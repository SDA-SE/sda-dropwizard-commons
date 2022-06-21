package org.sdase.commons.server.healthcheck;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.healthcheck.helper.ExternalServiceHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HealthCheckIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckIT.class);

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> EXT =
      new DropwizardAppExtension<>(HealthApplication.class, null, randomPorts());

  @BeforeEach
  public void setUp() {
    setApplicationHealth(true);
  }

  @Test
  void testAllHealthChecks() {
    try (Response response = healthCheckGet("/healthcheck")) {
      String metrics = response.readEntity(String.class);
      assertThat(metrics)
          .contains(HealthApplication.DUMMY_EXTERNAL)
          .contains(HealthApplication.DUMMY_INTERNAL);
    }
  }

  @Test
  void testHealthChecksInternal() {
    try (Response response = healthCheckGet("/healthcheck/internal")) {
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      String metrics = response.readEntity(String.class);
      LOGGER.info("Prometheus metrics: {}", metrics);

      assertThat(metrics)
          .doesNotContain(HealthApplication.DUMMY_EXTERNAL)
          .contains(HealthApplication.DUMMY_INTERNAL);
    }
  }

  @Test
  void shouldBeUnhealthy() {
    setApplicationHealth(false);
    try (Response response = healthCheckGet("/healthcheck")) {
      assertThat(response.getStatus()).isNotEqualTo(SC_OK);
      String metrics = response.readEntity(String.class);
      assertThat(metrics)
          .contains(HealthApplication.DUMMY_EXTERNAL)
          .contains(HealthApplication.DUMMY_INTERNAL);
    }
  }

  @StdIo
  @Test
  void shouldBeUnhealthyInternal(StdOut stdOut) {
    setApplicationHealth(false);
    try (Response response = healthCheckGet("/healthcheck/internal")) {
      assertThat(response.getStatus()).isNotEqualTo(SC_OK);

      String metrics = response.readEntity(String.class);
      assertThat(metrics)
          .doesNotContain(HealthApplication.DUMMY_EXTERNAL)
          .contains(HealthApplication.DUMMY_INTERNAL);

      String logs = String.join("\n", stdOut.capturedLines());
      assertThat(logs).contains("Internal healthcheck failed: " + HealthApplication.DUMMY_INTERNAL);
    }
  }

  @Test
  void testRegressionRunHealthCheckSuccessfulTwice() throws Exception {
    ExternalServiceHealthCheck healthCheck =
        new ExternalServiceHealthCheck(
            String.format("http://localhost:%d/metrics", EXT.getAdminPort()), 1000);

    HealthCheck.Result firstResult = healthCheck.check();
    HealthCheck.Result secondResult = healthCheck.check();

    assertThat(firstResult.isHealthy()).isTrue();
    assertThat(secondResult.isHealthy()).isTrue();
  }

  private void setApplicationHealth(boolean isHealthy) {
    HealthApplication app = EXT.getApplication();
    app.setHealthy(isHealthy);
  }

  private Response healthCheckGet(String s) {
    return EXT.client()
        .target(String.format("http://localhost:%d", EXT.getAdminPort()) + s)
        .request()
        .get();
  }
}
