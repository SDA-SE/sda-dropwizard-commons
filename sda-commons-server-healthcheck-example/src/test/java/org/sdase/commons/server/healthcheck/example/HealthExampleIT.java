package org.sdase.commons.server.healthcheck.example;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class HealthExampleIT {

  private static final String SERVICE_PATH = "/service"; // NOSONAR
  private static final String HEALTH_CHECK = "healthcheck";
  private static final String HEALTH_INTERNAL = "healthcheck/internal";

  // Wiremock to mock external service
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  // create example application
  public static final DropwizardAppRule<HealthExampleConfiguration> DW =
      new DropwizardAppRule<>(
          HealthExampleApplication.class,
          resourceFilePath("test-config.yaml"),
          config("externalServiceUrl", () -> WIRE.url(SERVICE_PATH)));

  // start the two rules in order
  @ClassRule public static final RuleChain RULE = RuleChain.outerRule(WIRE).around(DW);

  @Before
  public void resetMock() {
    WIRE.resetMappings();
  }

  private void mockHealthy() {
    WIRE.stubFor(get(SERVICE_PATH).willReturn(aResponse().withStatus(200)));
  }

  private void mockUnhealthy() {
    WIRE.stubFor(get(SERVICE_PATH).willReturn(aResponse().withStatus(500)));
  }

  @Test
  public void overallHealthCheckStatusShouldBecomeUnhealthy() throws InterruptedException {
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
  public void internalHealthCheckStatusShouldBecomeUnhealthy() throws InterruptedException {
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
  public void externalHealthCheckDoesOnlyAffectOverallStatus() {
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
