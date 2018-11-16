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

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class ApiClientTest {

   @ClassRule
   public static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private static final ObjectMapper OM = new ObjectMapper();

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
                  .withHeader("Accept", equalTo("application/json"))
                  .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-type", "application/json")
                        .withBody(OM.writeValueAsBytes(asList(
                              new Car().setSign("HH XX 1234").setColor("bright blue"),
                              new Car().setSign("HH XY 4321").setColor("light blue")
                              ))
                        )
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
            .withoutHeader(HttpHeaders.AUTHORIZATION)
      );
   }

   private MockApiClient createMockApiClient() {
      return app.getJerseyClientBundle().getClientFactory()
            .platformClient()
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
