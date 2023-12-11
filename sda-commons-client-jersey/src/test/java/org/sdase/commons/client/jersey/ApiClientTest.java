package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.seeOther;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_SEE_OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sdase.commons.client.jersey.error.ClientErrorUtil.convertExceptions;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.processingError;

import com.codahale.metrics.MetricFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.error.ClientErrorUtil;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.shared.api.error.ApiException;

public class ApiClientTest {

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE = new WireMockExtension.Builder().build();

  public static final Car LIGHT_BLUE_CAR =
      new Car().setSign("HH XY 4321").setColor("light blue"); // NOSONAR
  private static final ObjectMapper OM = new ObjectMapper();
  private static final Car BRIGHT_BLUE_CAR =
      new Car().setSign("HH XX 1234").setColor("bright blue"); // NOSONAR

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE::baseUrl));

  private ClientTestApp app;

  @BeforeAll
  static void beforeAll() {
    //    Explicitly set the wiremock host and port, to keep configuration after reset in setUp()
    WireMock.configureFor("http", "localhost", WIRE.getPort());
  }

  @BeforeEach
  void before() throws JsonProcessingException {
    WIRE.resetAll();
    app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);

    WIRE.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", equalTo("application/json")) // NOSONAR
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json") // NOSONAR
                    .withBody(OM.writeValueAsBytes(asList(BRIGHT_BLUE_CAR, LIGHT_BLUE_CAR)))));
    WIRE.stubFor(
        post("/api/multi-part") // NOSONAR
            .withHeader("Content-type", matching("multipart/form-data.*")) // NOSONAR
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-type", "application/json") // NOSONAR
                    .withBody(OM.writeValueAsBytes(singletonMap("got", "it")))));
    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .withHeader("Content-type", equalTo("application/json")) // NOSONAR
            .withRequestBody(equalToJson("{\"sign\":\"HH AB 1234\", \"color\":\"white\"}"))
            .willReturn(
                aResponse().withStatus(201).withHeader("Location", "/api/cars/dummyId") // NOSONAR
                ));
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
  void sendMultipart() throws IOException {
    try (FormDataMultiPart multiPart =
        new FormDataMultiPart()
            .field("test", "test-value", MediaType.TEXT_PLAIN_TYPE)
            .field("anotherTest", "another-test-value", MediaType.TEXT_PLAIN_TYPE)) {
      try (Response response =
          app.getJerseyClientBundle()
              .getClientFactory()
              .platformClient()
              .buildGenericClient("multi-part")
              .target(WIRE.baseUrl())
              .path("api")
              .path("multi-part")
              .register(MultiPartFeature.class)
              .request(MediaType.APPLICATION_JSON)
              .post(Entity.entity(multiPart, multiPart.getMediaType()))) {
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("Content-type")).isEqualTo("application/json");
      }

      String contentType =
          WIRE.getAllServeEvents().get(0).getRequest().contentTypeHeader().firstValue();
      String boundary = MediaType.valueOf(contentType).getParameters().get("boundary");
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlEqualTo("/api/multi-part"))
              .withHeader("Content-type", matching("multipart/form-data;boundary=.+"))
              .withRequestBody(
                  equalTo(
                      "--"
                          + boundary
                          + "\r\n"
                          + "Content-Type: text/plain\r\n"
                          + // NOSONAR
                          "Content-Disposition: form-data; name=\"test\"\r\n"
                          + "\r\n"
                          + "test-value\r\n"
                          + "--"
                          + boundary
                          + "\r\n"
                          + "Content-Type: text/plain\r\n"
                          + "Content-Disposition: form-data; name=\"anotherTest\"\r\n"
                          + "\r\n"
                          + "another-test-value\r\n"
                          + "--"
                          + boundary
                          + "--\r\n")));
    }
  }

  @Test
  void sendMultipartWithApiClient() throws IOException {
    try (FormDataMultiPart multiPart =
        new FormDataMultiPart()
            .field("test", "test-value", MediaType.TEXT_PLAIN_TYPE)
            .field("anotherTest", "another-test-value", MediaType.TEXT_PLAIN_TYPE)) {
      try (Response response = createMockApiClient().sendMultiPart(multiPart)) {
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("Content-type")).isEqualTo("application/json");
      }

      String contentType =
          WIRE.getAllServeEvents().get(0).getRequest().contentTypeHeader().firstValue();
      String boundary = MediaType.valueOf(contentType).getParameters().get("boundary");
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlEqualTo("/api/multi-part"))
              .withHeader("Content-type", matching("multipart/form-data;boundary=.+"))
              .withRequestBody(
                  equalTo(
                      "--"
                          + boundary
                          + "\r\n"
                          + "Content-Type: text/plain\r\n"
                          + "Content-Disposition: form-data; name=\"test\"\r\n"
                          + "\r\n"
                          + "test-value\r\n"
                          + "--"
                          + boundary
                          + "\r\n"
                          + "Content-Type: text/plain\r\n"
                          + "Content-Disposition: form-data; name=\"anotherTest\"\r\n"
                          + "\r\n"
                          + "another-test-value\r\n"
                          + "--"
                          + boundary
                          + "--\r\n")));
    }
  }

  @Test
  void loadCars() {
    List<Car> cars = createMockApiClient().getCars();

    assertThat(cars)
        .extracting(Car::getSign, Car::getColor)
        .containsExactly(tuple("HH XX 1234", "bright blue"), tuple("HH XY 4321", "light blue"));
  }

  @Test
  void loadCarsWithResponseDetails() {
    try (Response response = createMockApiClient().requestCars()) {

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withHeader("Trace-Token", matchingUuid()) // NOSONAR
              .withoutHeader(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void loadCarsWithBasicAuthAndResponseDetails() {
    try (Response response = createMockApiClientWithBasicAuth().requestCars()) {

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withHeader("Trace-Token", matchingUuid()) // NOSONAR
              .withHeader(HttpHeaders.AUTHORIZATION, matching("Basic Zm9vOmJhcg==")));
    }
  }

  @Test
  void addConsumerToken() {
    try (Response response = createMockApiClient().requestCars()) {

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withHeader("Consumer-Token", equalTo("test-consumer")) // NOSONAR
              .withoutHeader(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void postNewCar() {
    try (Response response =
        createMockApiClient().createCar(new Car().setSign("HH AB 1234").setColor("white"))) {

      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation()).isEqualTo(URI.create("/api/cars/dummyId"));
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlEqualTo("/api/cars"))
              .withHeader("Content-type", equalTo("application/json")) // NOSONAR
          );
    }
  }

  @Test
  void notAddConsumerTokenIfAlreadySet() {
    try (Response response =
        createMockApiClient().requestCarsWithCustomConsumerToken("my-custom-consumer")) {

      assertThat(response.getStatus()).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withHeader("Consumer-Token", equalTo("my-custom-consumer"))
              .withHeader("Consumer-Token", notMatching("test-consumer"))
              .withoutHeader(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void addReceivedTraceTokenToHeadersToPlatformCall() {
    try (Response response =
        dwClient()
            .path("api")
            .path("cars")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Trace-Token", "test-trace-token-1")
            .get()) {
      int status = response.getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withHeader("Trace-Token", equalTo("test-trace-token-1"))
              .withoutHeader(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void addReceivedAuthHeaderToPlatformCall() {
    try (Response response =
        dwClient()
            .path("api")
            .path("carsAuth")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "custom-dummy-token")
            .header("Trace-Token", "test-trace-token-3")
            .get()) {
      int status = response.getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withHeader("Trace-Token", equalTo("test-trace-token-3"))
              .withHeader(HttpHeaders.AUTHORIZATION, equalTo("custom-dummy-token")));
    }
  }

  @Test
  void notAddingReceivedTraceTokenToHeadersOfExternalCall() {
    try (Response response =
        dwClient()
            .path("api")
            .path("carsExternal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Trace-Token", "test-trace-token-2")
            .get()) {
      int status = response.getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withoutHeader("Trace-Token"));
    }
  }

  @Test
  void notAddingReceivedAuthorizationToHeadersOfExternalCall() {
    try (Response response =
        dwClient()
            .path("api")
            .path("carsExternal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "BEARER dummy")
            .get()) {
      int status = response.getStatus();

      assertThat(status).isEqualTo(200);
      WIRE.verify(
          RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
              .withoutHeader(HttpHeaders.AUTHORIZATION));
    }
  }

  @Test
  void loadSingleCar() {
    Car car = createMockApiClient().getCar("HH XY 4321");
    assertThat(car)
        .extracting(Car::getSign, Car::getColor)
        .containsExactly("HH XY 4321", "light blue");
  }

  @Test
  void loadSingleCarWithFieldFilterAllFields() {
    createMockApiClient().getCarWithFilteredFields("HH XY 4321", asList("sign", "color"));
    verify(
        RequestPatternBuilder.newRequestPattern(
            GET, urlEqualTo("/api/cars/HH%20XY%204321?fields=sign&fields=color")));
  }

  @Test
  void loadSingleCarWithFieldFilterOneField() {
    createMockApiClient().getCarWithFilteredFields("HH XY 4321", singletonList("color"));
    verify(
        RequestPatternBuilder.newRequestPattern(
            GET, urlEqualTo("/api/cars/HH%20XY%204321?fields=color")));
  }

  @Test
  void loadLightBlueCarThroughDefaultMethod() {
    try (Response response = createMockApiClient().getLightBlueCar()) {
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.readEntity(Car.class))
          .extracting(Car::getSign, Car::getColor)
          .containsExactly("HH XY 4321", "light blue");
    }
  }

  @Test
  void handle404ErrorInDefaultMethod() {
    WIRE.stubFor(
        get("/api/cars/UNKNOWN")
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{}")));
    assertThat(createMockApiClient().getCarOrHandleError("UNKNOWN")).isNull();
  }

  @Test
  void handleClientErrorInDefaultMethod() {
    WIRE.stubFor(
        get("/api/cars/UNKNOWN")
            .willReturn(
                aResponse()
                    .withStatus(500)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{}")));
    assertThatExceptionOfType(ApiException.class)
        .isThrownBy(() -> createMockApiClient().getCarOrHandleError("UNKNOWN"))
        .withCauseInstanceOf(ClientRequestException.class);
  }

  @Test
  void failOnLoadingMissingCar() {
    assertThatExceptionOfType(ClientRequestException.class)
        .isThrownBy(() -> createMockApiClient().getCar("HH AA 4444"))
        .withCauseInstanceOf(NotFoundException.class);
  }

  @Test
  void demonstrateToCloseException() {
    WIRE.stubFor(
        get("/api/cars/HH%20AA%204444")
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{ 'message': 'car is unknown' }")));

    try {
      createMockApiClient().getCar("HH AA 4444");
    } catch (ClientRequestException e) {
      assertThat(e.getResponse().map(Response::getStatus)).isPresent().hasValue(404);
      assertThat(e.getResponse().map(Response::hasEntity)).isPresent().hasValue(true);
      e.close(); // important if exception is not rethrown
    }
  }

  @Test
  void return404ForMissingCar() {
    try (Response response = createMockApiClient().getCarResponse("HH AA 5555")) {
      assertThat(response.getStatus()).isEqualTo(404);
    }
  }

  @Test
  void return404ForMissingCarWithGenericClient() {
    try (Response response =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .buildGenericClient("foo")
            .target(WIRE.baseUrl())
            .path("api")
            .path("cars")
            .path("HH AA 7777")
            .request(MediaType.APPLICATION_JSON)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(404);
    }
  }

  @Test
  void return500ForDelegatedMissingCar() {
    try (Response response =
        dwClient()
            .path("api")
            .path("cars")
            .path("HH AA 7777")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get()) {
      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(ClientErrorUtil.readErrorBody(response, new GenericType<Map<String, Object>>() {}))
          .containsExactly(
              entry(
                  "title",
                  "Request could not be fulfilled: Received status '404' from another service."),
              entry("invalidParams", Collections.emptyList()));
    }
  }

  @Test
  void addCustomFiltersToPlatformClient() {
    MockApiClient mockApiClient =
        app.getJerseyClientBundle()
            .getClientFactory()
            .platformClient()
            .addFilter(
                requestContext -> requestContext.getHeaders().add("Hello", "World")) // NOSONAR
            .addFilter(requestContext -> requestContext.getHeaders().add("Foo", "Bar"))
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    mockApiClient.getCars();

    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Hello", equalTo("World"))
            .withHeader("Foo", equalTo("Bar")));
  }

  @Test
  void addCustomFiltersToExternalClient() {
    MockApiClient mockApiClient =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .addFilter(requestContext -> requestContext.getHeaders().add("Hello", "World"))
            .addFilter(requestContext -> requestContext.getHeaders().add("Foo", "Bar"))
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    mockApiClient.getCars();

    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Hello", equalTo("World"))
            .withHeader("Foo", equalTo("Bar")));
  }

  @Test
  void failOnReadJsonWithGenericClient() {

    WIRE.stubFor(
        get("/badJsonResponse")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{ \"sign\": \"xyz\"")));

    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");

    assertThatExceptionOfType(ClientRequestException.class)
        .isThrownBy(
            () ->
                // receives invalid json
                convertExceptions(
                    () ->
                        client
                            .target(WIRE.baseUrl())
                            .path("badJsonResponse")
                            .request()
                            .get(Car.class)))
        .is(processingError());
  }

  @Test
  void failOnReadJsonWithApiClient() {

    WIRE.stubFor(
        get("/api/cars/123")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{ \"sign\": \"xyz\"")));

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    assertThatExceptionOfType(ClientRequestException.class)
        .isThrownBy(() -> client.getCar("123"))
        .is(processingError());
  }

  @Test
  void seeOtherAfterPost() {
    WIRE.stubFor(
        post("/api/cars")
            .withRequestBody(
                equalToJson("{ \"sign\": \"HH XY 1234\", \"color\": \"yellow\" }")) // NOSONAR
            .willReturn(seeOther(WIRE.url("/api/cars/HH%20XY%201234")))); // NOSONAR
    WIRE.stubFor(
        get("/api/cars/HH%20XY%201234")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{ \"sign\": \"HH XY 1234\", \"color\": \"yellow\" }")));

    // This test requires that gzip for request bodies is disabled, otherwise
    // the automatic GET request after the see others fails as the
    // content-encoding from the post request is also used for the GET
    // request. However neither Dropwizard nor Wiremock does handle this well
    // for GET requests.
    MockApiClient client = createMockApiClient();

    try (Response r =
        client.createCar(new Car().setSign("HH XY 1234").setColor("yellow"))) { // NOSONAR
      assertThat(r.getStatus()).isEqualTo(SC_OK);
      Car car = r.readEntity(Car.class);
      assertThat(car)
          .extracting(Car::getSign, Car::getColor)
          .containsExactly("HH XY 1234", "yellow");
    }
  }

  @Test
  void disableRedirectsForSeeOther() {
    WIRE.stubFor(
        post("/api/cars")
            .withRequestBody(equalToJson("{ \"sign\": \"HH XY 1234\", \"color\": \"yellow\" }"))
            .willReturn(seeOther(WIRE.url("/api/cars/HH%20XY%201234"))));
    WIRE.stubFor(
        get("/api/cars/HH%20XY%201234")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{ \"sign\": \"HH XY 1234\", \"color\": \"yellow\" }")));

    MockApiClient client = createMockApiClientWithoutFollowRedirects();

    try (Response r = client.createCar(new Car().setSign("HH XY 1234").setColor("yellow"))) {
      assertThat(r.getStatus()).isEqualTo(SC_SEE_OTHER);
      assertThat(r.getHeaderString(HttpHeaders.LOCATION))
          .isEqualTo(WIRE.url("/api/cars/HH%20XY%201234"));
      WIRE.verify(
          0, RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars/HH%20XY%201234")));
    }
  }

  private MockApiClient createMockApiClientWithoutFollowRedirects() {
    return app.getJerseyClientBundle()
        .getClientFactory()
        .platformClient()
        .disableFollowRedirects()
        .enableConsumerToken()
        .api(MockApiClient.class)
        .atTarget(WIRE.baseUrl());
  }

  private MockApiClient createMockApiClient() {
    return app.getJerseyClientBundle()
        .getClientFactory()
        .platformClient()
        .enableConsumerToken()
        .api(MockApiClient.class)
        .atTarget(WIRE.baseUrl());
  }

  private MockApiClient createMockApiClientWithBasicAuth() {
    return app.getJerseyClientBundle()
        .getClientFactory()
        .platformClient()
        .addFeature(HttpAuthenticationFeature.basic("foo", "bar"))
        .enableConsumerToken()
        .api(MockApiClient.class)
        .atTarget(WIRE.baseUrl());
  }

  private WebTarget dwClient() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }

  private StringValuePattern matchingUuid() {
    return matching(
        "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
  }
}
