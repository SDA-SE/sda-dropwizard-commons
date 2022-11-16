package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.clients.apia.Car;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class JerseyClientExampleIT {

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<JerseyClientExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          JerseyClientExampleApplication.class,
          resourceFilePath("test-config.yaml"),
          config("servicea", () -> WIRE.url("api")));

  private JerseyClientExampleApplication app;
  private static final ObjectMapper OM = new ObjectMapper();
  private static final Car BRIGHT_BLUE_CAR =
      new Car().setSign("HH XX 1234").setColor("bright blue"); // NOSONAR
  private static final Car LIGHT_BLUE_CAR =
      new Car().setSign("HH XY 4321").setColor("light blue"); // NOSONAR

  @BeforeEach
  void resetRequests() {
    WIRE.resetRequests();
    app = DW.getApplication();
  }

  @BeforeAll
  public static void initWires() throws JsonProcessingException {
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
