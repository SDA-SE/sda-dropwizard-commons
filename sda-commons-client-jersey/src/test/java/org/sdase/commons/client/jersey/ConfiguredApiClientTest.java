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
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SEE_OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.groups.Tuple.tuple;
import static org.sdase.commons.client.jersey.error.ClientErrorUtil.convertExceptions;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.processingError;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.jetty9.JettyHttpServerFactory;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.ClassRule;
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
import org.sdase.commons.shared.api.error.ApiException;

public class ConfiguredApiClientTest {

  @ClassRule
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(
          wireMockConfig().dynamicPort().httpServerFactory(new JettyHttpServerFactory()));

  public static final Car LIGHT_BLUE_CAR =
      new Car().setSign("HH XY 4321").setColor("light blue"); // NOSONAR
  private static final ObjectMapper OM = new ObjectMapper();
  private static final Car BRIGHT_BLUE_CAR =
      new Car().setSign("HH XX 1234").setColor("bright blue"); // NOSONAR
  private final DropwizardAppRule<ClientTestConfig> dw =
      new DropwizardAppRule<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(new EnvironmentRule().setEnv("MOCK_BASE_URL", WIRE.baseUrl())).around(dw);

  private ClientTestApp app;

  @Before
  public void before() throws JsonProcessingException {
    WIRE.resetAll();
    app = dw.getApplication();

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
  public void sendMultipart() throws IOException {
    try (FormDataMultiPart multiPart =
        new FormDataMultiPart()
            .field("test", "test-value", MediaType.TEXT_PLAIN_TYPE)
            .field("anotherTest", "another-test-value", MediaType.TEXT_PLAIN_TYPE)) {
      Response response =
          app.getJerseyClientBundle()
              .getClientFactory()
              .platformClient()
              .buildGenericClient("multi-part")
              .target(WIRE.baseUrl())
              .path("api")
              .path("multi-part")
              .register(MultiPartFeature.class)
              .request(MediaType.APPLICATION_JSON)
              .post(Entity.entity(multiPart, multiPart.getMediaType()));
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeaderString("Content-type")).isEqualTo("application/json");

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
  public void sendMultipartWithApiClient() throws IOException {
    try (FormDataMultiPart multiPart =
        new FormDataMultiPart()
            .field("test", "test-value", MediaType.TEXT_PLAIN_TYPE)
            .field("anotherTest", "another-test-value", MediaType.TEXT_PLAIN_TYPE)) {
      Response response = createMockApiClient().sendMultiPart(multiPart);
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.getHeaderString("Content-type")).isEqualTo("application/json");

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
  public void loadCars() {
    List<Car> cars = createMockApiClient().getCars();

    assertThat(cars)
        .extracting(Car::getSign, Car::getColor)
        .containsExactly(tuple("HH XX 1234", "bright blue"), tuple("HH XY 4321", "light blue"));
  }

  @Test
  public void loadCarsWithResponseDetails() {
    Response response = createMockApiClient().requestCars();

    assertThat(response.getStatus()).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Trace-Token", matchingUuid()) // NOSONAR
            .withoutHeader(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void addConsumerToken() {
    Response response = createMockApiClient().requestCars();

    assertThat(response.getStatus()).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Consumer-Token", equalTo("test-consumer")) // NOSONAR
            .withoutHeader(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void postNewCar() {
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
  public void notAddConsumerTokenIfAlreadySet() {
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
  public void addReceivedTraceTokenToHeadersToPlatformCall() {
    int status =
        dwClient()
            .path("api")
            .path("cars")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Trace-Token", "test-trace-token-1")
            .get()
            .getStatus();

    assertThat(status).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Trace-Token", equalTo("test-trace-token-1"))
            .withoutHeader(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void addReceivedAuthHeaderToPlatformCall() {
    int status =
        dwClient()
            .path("api")
            .path("carsAuth")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Authorization", "custom-dummy-token")
            .header("Trace-Token", "test-trace-token-3")
            .get()
            .getStatus();

    assertThat(status).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Trace-Token", equalTo("test-trace-token-3"))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo("custom-dummy-token")));
  }

  @Test
  public void notAddingReceivedTraceTokenToHeadersOfExternalCall() {
    int status =
        dwClient()
            .path("api")
            .path("carsExternal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header("Trace-Token", "test-trace-token-2")
            .get()
            .getStatus();

    assertThat(status).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withoutHeader("Trace-Token"));
  }

  @Test
  public void notAddingReceivedAuthorizationToHeadersOfExternalCall() {
    int status =
        dwClient()
            .path("api")
            .path("carsExternal")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "BEARER dummy")
            .get()
            .getStatus();

    assertThat(status).isEqualTo(200);
    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withoutHeader(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void loadSingleCar() {
    Car car = createMockApiClient().getCar("HH XY 4321");
    assertThat(car)
        .extracting(Car::getSign, Car::getColor)
        .containsExactly("HH XY 4321", "light blue");
  }

  @Test
  public void loadSingleCarWithFieldFilterAllFields() {
    createMockApiClient().getCarWithFilteredFields("HH XY 4321", asList("sign", "color"));
    verify(
        RequestPatternBuilder.newRequestPattern(
            GET, urlEqualTo("/api/cars/HH%20XY%204321?fields=sign&fields=color")));
  }

  @Test
  public void loadSingleCarWithFieldFilterOneField() {
    createMockApiClient().getCarWithFilteredFields("HH XY 4321", singletonList("color"));
    verify(
        RequestPatternBuilder.newRequestPattern(
            GET, urlEqualTo("/api/cars/HH%20XY%204321?fields=color")));
  }

  @Test
  public void loadLightBlueCarThroughDefaultMethod() {
    try (Response response = createMockApiClient().getLightBlueCar()) {
      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(response.readEntity(Car.class))
          .extracting(Car::getSign, Car::getColor)
          .containsExactly("HH XY 4321", "light blue");
    }
  }

  @Test
  public void handle404ErrorInDefaultMethod() {
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
  public void handleClientErrorInDefaultMethod() {
    WIRE.stubFor(
        get("/api/cars/UNKNOWN")
            .willReturn(
                aResponse()
                    .withStatus(500)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{}")));
    MockApiClient mockApiClient = createMockApiClient();
    assertThatExceptionOfType(ApiException.class)
        .isThrownBy(() -> mockApiClient.getCarOrHandleError("UNKNOWN"))
        .withCauseInstanceOf(ClientRequestException.class);
  }

  @Test
  public void failOnLoadingMissingCar() {
    MockApiClient mockApiClient = createMockApiClient();
    assertThatExceptionOfType(ClientRequestException.class)
        .isThrownBy(() -> mockApiClient.getCar("HH AA 4444"))
        .withCauseInstanceOf(NotFoundException.class);
  }

  @Test
  public void demonstrateToCloseException() {
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
  public void return404ForMissingCar() {
    try (Response response = createMockApiClient().getCarResponse("HH AA 5555")) {
      assertThat(response.getStatus()).isEqualTo(404);
    }
  }

  @Test
  public void return404ForMissingCarWithGenericClient() {
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
  public void return500ForDelegatedMissingCar() {
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
              entry("invalidParams", Lists.emptyList()));
    }
  }

  @Test
  public void addCustomFiltersToPlatformClient() {
    MockApiClient mockApiClient =
        app.getJerseyClientBundle()
            .getClientFactory()
            .apiClient(MockApiClient.class)
            .withConfiguration(new ApiHttpClientConfiguration().setApiBaseUrl(WIRE.baseUrl()))
            .addFilter(
                requestContext -> requestContext.getHeaders().add("Hello", "World")) // NOSONAR
            .addFilter(requestContext -> requestContext.getHeaders().add("Foo", "Bar"))
            .enablePlatformFeatures()
            .build();

    mockApiClient.getCars();

    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Hello", equalTo("World"))
            .withHeader("Foo", equalTo("Bar")));
  }

  @Test
  public void addCustomFiltersToExternalClient() {
    MockApiClient mockApiClient =
        app.getJerseyClientBundle()
            .getClientFactory()
            .apiClient(MockApiClient.class)
            .withConfiguration(new ApiHttpClientConfiguration().setApiBaseUrl(WIRE.baseUrl()))
            .addFilter(requestContext -> requestContext.getHeaders().add("Hello", "World"))
            .addFilter(requestContext -> requestContext.getHeaders().add("Foo", "Bar"))
            .build();

    mockApiClient.getCars();

    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(GET, urlEqualTo("/api/cars"))
            .withHeader("Hello", equalTo("World"))
            .withHeader("Foo", equalTo("Bar")));
  }

  @Test
  public void failOnReadJsonWithGenericClient() {

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
  public void failOnReadJsonWithApiClient() {

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
            .apiClient(MockApiClient.class)
            .withConfiguration(new ApiHttpClientConfiguration().setApiBaseUrl(WIRE.baseUrl()))
            .build();

    assertThatExceptionOfType(ClientRequestException.class)
        .isThrownBy(() -> client.getCar("123"))
        .is(processingError());
  }

  @Test
  public void seeOtherAfterPost() {
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
  public void disableRedirectsForSeeOther() {
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
        .apiClient(MockApiClient.class)
        .withConfiguration(new ApiHttpClientConfiguration().setApiBaseUrl(WIRE.baseUrl()))
        .disableFollowRedirects()
        .enablePlatformFeatures()
        .enableConsumerToken()
        .build();
  }

  private MockApiClient createMockApiClient() {
    return app.getJerseyClientBundle()
        .getClientFactory()
        .apiClient(MockApiClient.class)
        .withConfiguration(new ApiHttpClientConfiguration().setApiBaseUrl(WIRE.baseUrl()))
        .enablePlatformFeatures()
        .enableConsumerToken()
        .enableAuthenticationPassThrough()
        .build();
  }

  private WebTarget dwClient() {
    return dw.client().target("http://localhost:" + dw.getLocalPort());
  }

  private StringValuePattern matchingUuid() {
    return matching(
        "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");
  }
}
