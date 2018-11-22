package org.sdase.commons.client.jersey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.server.testing.EnvironmentRule;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;

public class ApiClientTest {

   @ClassRule
   public static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private static final ObjectMapper OM = new ObjectMapper();
   private static final Car BRIGHT_BLUE_CAR = new Car().setSign("HH XX 1234").setColor("bright blue"); // NOSONAR
   private static final Car LIGHT_BLUE_CAR = new Car().setSign("HH XY 4321").setColor("light blue"); // NOSONAR

   private final DropwizardAppRule<ClientTestConfig> dw = new DropwizardAppRule<>(
         ClientTestApp.class, resourceFilePath("test-config.yaml"));

   @Rule
   public final RuleChain rule = RuleChain
         .outerRule(new EnvironmentRule().setEnv("MOCK_BASE_URL", WIRE.baseUrl()))
         .around(dw);

   private ClientTestApp app;

   @BeforeClass
   public static void initWires() throws JsonProcessingException {
      WIRE.start();
      WIRE.stubFor(
            get("/api/cars") // NOSONAR
                  .withHeader("Accept", equalTo("application/json")) // NOSONAR
                  .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "application/json") // NOSONAR
                        .withBody(OM.writeValueAsBytes(asList(BRIGHT_BLUE_CAR, LIGHT_BLUE_CAR))
                        )
                  )
      );
      WIRE.stubFor(
            get("/api/cars/HH%20XX%201234")
                  .withHeader("Accept", equalTo("application/json"))
                  .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "application/json")
                        .withBody(OM.writeValueAsBytes(BRIGHT_BLUE_CAR))
                  )
      );
      WIRE.stubFor(
            get("/api/cars/HH%20XY%204321")
                  .withHeader("Accept", equalTo("application/json"))
                  .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "application/json")
                        .withBody(OM.writeValueAsBytes(LIGHT_BLUE_CAR))
                  )
      );
   }

   @Before
   public void resetRequests() {
      WIRE.resetRequests();
      app = dw.getApplication();
   }

   @Test
   public void loadCars() {
      List<Car> cars = createMockApiClient().getCars();

      assertThat(cars).extracting(Car::getSign, Car::getColor)
            .containsExactly(tuple("HH XX 1234", "bright blue"), tuple("HH XY 4321", "light blue"));
   }

   @Test
   public void loadCarsWithResponseDetails() {
      Response response = createMockApiClient().requestCars();

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withHeader("Trace-Token", matchingUuid()) // NOSONAR
            .withoutHeader(HttpHeaders.AUTHORIZATION)
      );
   }

   @Test
   public void addConsumerToken() {
      Response response = createMockApiClient().requestCars();

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withHeader("Consumer-Token", equalTo("test-consumer")) // NOSONAR
            .withoutHeader(HttpHeaders.AUTHORIZATION)
      );
   }

   @Test
   public void notAddConsumerTokenIfAlreadySet() {
      Response response = createMockApiClient().requestCarsWithCustomConsumerToken("my-custom-consumer");

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withHeader("Consumer-Token", equalTo("my-custom-consumer"))
            .withHeader("Consumer-Token", notMatching("test-consumer"))
            .withoutHeader(HttpHeaders.AUTHORIZATION)
      );
   }

   @Test
   public void addReceivedTraceTokenToHeadersToPlatformCall() {
      int status = dwClient().path("api").path("cars")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Trace-Token", "test-trace-token-1")
            .get().getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
                  .withHeader("Trace-Token", equalTo("test-trace-token-1"))
                  .withoutHeader(HttpHeaders.AUTHORIZATION)
      );
   }

   @Test
   public void addReceivedAuthHeaderToPlatformCall() {
      int status = dwClient().path("api").path("carsAuth")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "custom-dummy-token")
            .header("Trace-Token", "test-trace-token-3")
            .get().getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
                  .withHeader("Trace-Token", equalTo("test-trace-token-3"))
                  .withHeader(HttpHeaders.AUTHORIZATION, equalTo("custom-dummy-token"))
      );
   }

   @Test
   public void notAddingReceivedTraceTokenToHeadersOfExternalCall() {
      int status = dwClient().path("api").path("carsExternal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Trace-Token", "test-trace-token-2")
            .get().getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withoutHeader("Trace-Token")
      );
   }

   @Test
   public void notAddingReceivedAuthorizationToHeadersOfExternalCall() {
      int status = dwClient().path("api").path("carsExternal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "BEARER dummy")
            .get().getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
                  .withoutHeader(HttpHeaders.AUTHORIZATION)
      );
   }

   @Test
   public void loadSingleCar() {
      Car car = createMockApiClient().getCar("HH XY 4321");
      assertThat(car).extracting(Car::getSign, Car::getColor).containsExactly("HH XY 4321", "light blue");
   }

   @Test
   public void failOnLoadingMissingCar() {
      assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> createMockApiClient().getCar("HH AA 4444"));
   }

   @Test
   public void return404ForMissingCar() {
      Response response = createMockApiClient().getCarResponse("HH AA 4444");
      assertThat(response.getStatus()).isEqualTo(404);
   }

   @Test
   public void addCustomFiltersToPlatformClient() {
      MockApiClient mockApiClient = app.getJerseyClientBundle().getClientFactory().platformClient()
            .addFilter(requestContext -> requestContext.getHeaders().add("Hello", "World")) // NOSONAR
            .addFilter(requestContext -> requestContext.getHeaders().add("Foo", "Bar"))
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

      mockApiClient.getCars();

      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withHeader("Hello", equalTo("World"))
            .withHeader("Foo", equalTo("Bar"))
      );
   }

   @Test
   public void addCustomFiltersToExternalClient() {
      MockApiClient mockApiClient = app.getJerseyClientBundle().getClientFactory().externalClient()
            .addFilter(requestContext -> requestContext.getHeaders().add("Hello", "World"))
            .addFilter(requestContext -> requestContext.getHeaders().add("Foo", "Bar"))
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

      mockApiClient.getCars();

      WIRE.verify(
            RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/api/cars"))
            .withHeader("Hello", equalTo("World"))
            .withHeader("Foo", equalTo("Bar"))
      );
   }

   private MockApiClient createMockApiClient() {
      return app.getJerseyClientBundle().getClientFactory()
            .platformClient()
            .enableConsumerToken()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());
   }

   private WebTarget dwClient() {
      return dw.client().target("http://localhost:" + dw.getLocalPort());
   }

   private StringValuePattern matchingUuid() {
      return matching("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
   }
}
