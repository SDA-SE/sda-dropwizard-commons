package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.ClientWithoutConsumerTokenTestApp;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.server.testing.EnvironmentRule;

public class ApiClientWithoutConsumerTokenTest {

  @ClassRule
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final ObjectMapper OM = new ObjectMapper();
  private static final Car BRIGHT_BLUE_CAR =
      new Car().setSign("HH XX 1234").setColor("bright blue"); // NOSONAR
  private static final Car LIGHT_BLUE_CAR =
      new Car().setSign("HH XY 4321").setColor("light blue"); // NOSONAR

  private final DropwizardAppRule<ClientTestConfig> dw =
      new DropwizardAppRule<>(
          ClientWithoutConsumerTokenTestApp.class, resourceFilePath("test-config.yaml"));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(new EnvironmentRule().setEnv("MOCK_BASE_URL", WIRE.baseUrl())).around(dw);

  private ClientWithoutConsumerTokenTestApp app;

  @BeforeClass
  public static void initWires() throws JsonProcessingException {
    WIRE.start();
    WIRE.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", equalTo("application/json")) // NOSONAR
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json") // NOSONAR
                    .withBody(OM.writeValueAsBytes(asList(BRIGHT_BLUE_CAR, LIGHT_BLUE_CAR)))));
  }

  @Before
  public void resetRequests() {
    WIRE.resetRequests();
    app = dw.getApplication();
  }

  @Test
  public void loadCars() {
    List<Car> cars = createMockApiClient().getCars();

    assertThat(cars)
        .extracting(Car::getSign, Car::getColor)
        .containsExactly(tuple("HH XX 1234", "bright blue"), tuple("HH XY 4321", "light blue"));
  }

  private MockApiClient createMockApiClient() {
    return app.getJerseyClientBundle()
        .getClientFactory()
        .platformClient()
        .enableConsumerToken()
        .api(MockApiClient.class)
        .atTarget(WIRE.baseUrl());
  }
}
