package org.sdase.commons.server.healthcheck.example;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sdase.commons.server.testing.DropwizardRuleHelper;

public class HealthExampleIT {

  private static final String SERVICE_PATH = "/service"; // NOSONAR
  private static final String HEALTH_CHECK = "healthcheck";
  private static final String HEALTH_INTERNAL = "healthcheck/internal";

  @ClassRule
  // Wiremock to mock external service
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  @Rule
  // create example application
  public final DropwizardAppRule<HealthExampleConfiguration> rule =
      DropwizardRuleHelper.dropwizardTestAppFrom(HealthExampleApplication.class)
          .withConfigFrom(HealthExampleConfiguration::new)
          .withRandomPorts()
          .withConfigurationModifier(c -> c.setExternalServiceUrl(WIRE.baseUrl() + SERVICE_PATH))
          .build();

  @BeforeClass
  public static void startMock() {
    WIRE.start();
  }

  @AfterClass
  public static void stopMock() {
    WIRE.stop();
  }

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
  public void overallHealthCheckStatusShouldBecomeUnhealthy() {
    mockHealthy();
    HealthExampleApplication app = rule.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealth(HEALTH_CHECK).getStatus()).isEqualTo(200);

    // stop counting thread and set application to unhealthy by doing so
    app.stopCountingThread();
    assertThat(getHealth(HEALTH_CHECK).getStatus()).isNotEqualTo(200);
  }

  @Test
  public void internalHealthCheckStatusShouldBecomeUnhealthy() {
    mockHealthy();
    HealthExampleApplication app = rule.getApplication();
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
    HealthExampleApplication app = rule.getApplication();
    // reset counting thread and set application to healthy by doing so
    app.startCountingThread();
    assertThat(getHealth(HEALTH_CHECK).getStatus()).isEqualTo(500);
    assertThat(getHealth(HEALTH_INTERNAL).getStatus()).isEqualTo(200);
  }

  private Response getHealth(String s) {
    return rule.client()
        .target("http://localhost:" + rule.getAdminPort())
        .path(s)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();
  }
}
