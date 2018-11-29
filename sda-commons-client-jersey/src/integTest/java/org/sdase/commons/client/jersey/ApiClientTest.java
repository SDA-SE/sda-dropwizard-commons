package org.sdase.commons.client.jersey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.http.conn.ConnectTimeoutException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.error.ClientErrorUtil;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.server.testing.EnvironmentRule;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;

public class ApiClientTest {

   @ClassRule
   public static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private static final ObjectMapper OM = new ObjectMapper();
   private static final Car BRIGHT_BLUE_CAR = new Car().setSign("HH XX 1234").setColor("bright blue"); // NOSONAR
   public static final Car LIGHT_BLUE_CAR = new Car().setSign("HH XY 4321").setColor("light blue"); // NOSONAR

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
   @Ignore("Default methods in API client interfaces are not supported by Jersey. A custom proxy may fix this later.")
   public void loadLightBlueCarThroughDefaultMethod() {
      Response response = createMockApiClient().getLightBlueCar();
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.readEntity(Car.class))
            .extracting(Car::getSign, Car::getColor)
            .containsExactly("HH XY 4321", "light blue");
   }

   @Test
   public void failOnLoadingMissingCar() {
      assertThatExceptionOfType(ClientRequestException.class)
            .isThrownBy(() -> createMockApiClient().getCar("HH AA 4444"))
            .withCauseInstanceOf(NotFoundException.class);
   }

   @Test
   public void return404ForMissingCar() {
      Response response = createMockApiClient().getCarResponse("HH AA 5555");
      assertThat(response.getStatus()).isEqualTo(404);
   }

   @Test
   public void return404ForMissingCarWithGenericClient() {
      Response response = app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("foo")
            .target(WIRE.baseUrl()).path("api").path("cars").path("HH AA 7777")
            .request(MediaType.APPLICATION_JSON)
            .get();
      assertThat(response.getStatus()).isEqualTo(404);
   }

   @Test
   public void return503ForDelegatedMissingCar() {
      Response response = dwClient().path("api").path("cars").path("HH AA 7777")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();
      assertThat(response.getStatus()).isEqualTo(503);
      assertThat(ClientErrorUtil.readErrorBody(response, new GenericType<Map<String, Object>>() {}))
            .containsExactly(
                  entry("title", "Request could not be fulfilled: Received status '404' from another service."),
                  entry("invalidParams", Lists.emptyList())
            );
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

   @Test(timeout = 1_000)
   public void runIntoDefaultConnectionTimeoutOf500Millis() {
      Client client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .buildGenericClient("test");

      await().between(400, MILLISECONDS, 600, MILLISECONDS).pollDelay(20, MILLISECONDS)
            .untilAsserted(() ->
                  // TODO should we map this to a custom exception?
                  assertThatExceptionOfType(ProcessingException.class).isThrownBy(() ->
                        // try to connect to an ip address that is not routable
                        client.target("http://192.168.123.123").path("timeout").request().get() // NOSONAR
                  ).withCauseInstanceOf(ConnectTimeoutException.class)
            );
   }

   @Test(timeout = 300)
   public void runIntoConfiguredConnectionTimeout() {
      Client client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .withConnectionTimeout(Duration.ofMillis(10))
            .buildGenericClient("test");

      await().between(5, MILLISECONDS, 80, MILLISECONDS).pollDelay(2, MILLISECONDS)
            .untilAsserted(() ->
                  // TODO should we map this to a custom exception?
                  assertThatExceptionOfType(ProcessingException.class).isThrownBy(() ->
                        // try to connect to an ip address that is not routable
                        client.target("http://192.168.123.123").path("timeout").request().get()
                  ).withCauseInstanceOf(ConnectTimeoutException.class)
            );
   }

   @Test(timeout = 5000)
   public void runIntoDefaultReadTimeoutOf2Seconds() {

      WIRE.stubFor(get("/timeout")
            .willReturn(aResponse()
                  .withStatus(200)
                  .withBody("")
                  .withFixedDelay(3000)
            )
      );

      Client client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .buildGenericClient("test");

      await().between(1900, MILLISECONDS, 2200, MILLISECONDS).pollDelay(20, MILLISECONDS)
            .untilAsserted(() ->
                  // TODO should we map this to a custom exception?
                  assertThatExceptionOfType(ProcessingException.class).isThrownBy(() ->
                        // try to connect to an ip address that is not routable
                        client.target(WIRE.baseUrl()).path("timeout").request().get()
                  ).withCauseInstanceOf(SocketTimeoutException.class)
            );

   }

   @Test(timeout = 5000)
   public void runIntoConfiguredReadTimeoutOf100Millis() {

      WIRE.stubFor(get("/timeout")
            .willReturn(aResponse()
                  .withStatus(200)
                  .withBody("")
                  .withFixedDelay(200)
            )
      );

      Client client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .withReadTimeout(Duration.ofMillis(100))
            .buildGenericClient("test");

      await().between(40, MILLISECONDS, 170, MILLISECONDS).pollDelay(2, MILLISECONDS)
            .untilAsserted(() ->
                  // TODO should we map this to a custom exception?
                  assertThatExceptionOfType(ProcessingException.class).isThrownBy(() ->
                        // try to connect to an ip address that is not routable
                        client.target(WIRE.baseUrl()).path("timeout").request().get()
                  ).withCauseInstanceOf(SocketTimeoutException.class)
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
