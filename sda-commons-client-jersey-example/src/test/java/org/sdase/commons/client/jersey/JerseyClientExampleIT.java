package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.clients.apia.Car;

class JerseyClientExampleIT {

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<JerseyClientExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          JerseyClientExampleApplication.class,
          resourceFilePath("test-config.yaml"),
          config("servicea", () -> WIRE.url("api")));

  private JerseyClientExampleApplication app;
  private static final ObjectMapper OM = new ObjectMapper();
  private static final Car BRIGHT_BLUE_CAR = new Car("HH XX 1234", "bright blue");
  private static final Car LIGHT_BLUE_CAR = new Car("HH XY 4321", "light blue");

  @BeforeEach
  void beforeEach() throws JsonProcessingException {
    resetRequests();
    initWires();
  }

  void resetRequests() {
    WIRE.resetAll();
    app = DW.getApplication();
  }

  void initWires() throws JsonProcessingException {
    WIRE.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", equalTo("application/json")) // NOSONAR
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json") // NOSONAR
                    .withBody(OM.writeValueAsBytes(asList(BRIGHT_BLUE_CAR, LIGHT_BLUE_CAR)))));
    WIRE.stubFor(
        get(urlMatching("/api/cars/HH%20XX%201234(\\?.*)?"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json")
                    .withBody(OM.writeValueAsBytes(BRIGHT_BLUE_CAR))));
    WIRE.stubFor(
        get(urlMatching("/api/cars/HH%20XY%204321(\\?.*)?"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json")
                    .withBody(OM.writeValueAsBytes(LIGHT_BLUE_CAR))));
  }

  @Test
  void shouldUsePlatformClient() {
    assertThat(app.usePlatformClient()).isEqualTo(200);
  }

  @Test
  void shouldUseExternalClient() {
    assertThat(app.useExternalClient()).isEqualTo(200);
  }

  @Test
  void shouldUseConfiguredExternalClient() {
    assertThat(app.useConfiguredExternalClient()).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withHeader(HttpHeaders.USER_AGENT, equalTo("configured-client")));
  }

  @Test
  void shouldUseApiClient() {
    assertThat(app.useApiClient()).isNotEmpty();
  }
}
