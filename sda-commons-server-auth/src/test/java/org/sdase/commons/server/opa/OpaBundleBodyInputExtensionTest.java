package org.sdase.commons.server.opa;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.extension.OpaInputExtension;

public class OpaBundleBodyInputExtensionTest {
  private static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final DropwizardAppRule<TestConfiguration> DW_WITH_EXTENSION =
      new DropwizardAppRule<>(
          TestApplicationWithExtension.class,
          resourceFilePath("test-config-key-provider.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "with"),

          // relax the timeout to make tests more stable
          config("opa.opaClient.timeout", "1s"));

  private static final DropwizardAppRule<TestConfiguration> DW_WITHOUT_EXTENSION =
      new DropwizardAppRule<>(
          TestApplicationWithoutExtension.class,
          resourceFilePath("test-config-key-provider.yaml"),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "without"),

          // relax the timeout to make tests more stable
          config("opa.opaClient.timeout", "1s"));

  @ClassRule
  public static RuleChain CHAIN =
      RuleChain.outerRule(WIRE).around(DW_WITH_EXTENSION).around(DW_WITHOUT_EXTENSION);

  @BeforeClass
  public static void before() {
    WIRE.resetAll();

    WIRE.stubFor(
        post("/v1/data/with")
            .withRequestBody(matchingJsonPath("$.input.body.key", equalTo("value")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{\"result\": {\"allow\": true}}")));

    WIRE.stubFor(
        post("/v1/data/without")
            .withRequestBody(matchingJsonPath("$.input.body.key", WireMock.absent()))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{\"result\": {\"allow\": true}}")));
  }

  @Test
  public void testFailureWhenExtensionIsActivated() {
    // given
    WIRE.resetRequests();

    // when
    Response response =
        DW_WITH_EXTENSION
            .client()
            .target("http://localhost:" + DW_WITH_EXTENSION.getLocalPort())
            .request()
            .post(Entity.json(Collections.singletonMap("key", "value")));

    // then
    assertThat(WIRE.getAllServeEvents()).hasSize(1);
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(String.class)).contains("Received null input.");
  }

  @Test
  public void testSuccessWhenExtensionIsNotActivated() {
    // given
    WIRE.resetRequests();

    // when
    Response response =
        DW_WITHOUT_EXTENSION
            .client()
            .target("http://localhost:" + DW_WITHOUT_EXTENSION.getLocalPort())
            .request()
            .post(Entity.json(Collections.singletonMap("key", "value")));

    // then
    assertThat(WIRE.getAllServeEvents()).hasSize(1);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isEqualTo("value");
  }

  /**
   * An extension that reads the body properties and sends them as-is to the OPA (not recommended!).
   * This consumes the input entity such that it can _not_ be accessed in the actual endpoint later.
   */
  static class BodyInputExtension implements OpaInputExtension<Map<String, Object>> {
    private final ObjectMapper objectMapper;

    BodyInputExtension(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> createAdditionalInputContent(
        ContainerRequestContext requestContext) {
      try {
        // caution! this causes problems.
        return objectMapper.readValue(
            requestContext.getEntityStream(), new TypeReference<Map<String, Object>>() {});
      } catch (IOException ignored) {
        return null;
      }
    }
  }

  /** A test application without any additional extension. */
  public static class TestApplicationWithoutExtension extends TestApplication {
    public TestApplicationWithoutExtension() {
      super(false);
    }
  }

  /** A test application with the BodyInputExtension extension. */
  public static class TestApplicationWithExtension extends TestApplication {
    public TestApplicationWithExtension() {
      super(true);
    }
  }

  abstract static class TestApplication extends Application<TestConfiguration> {
    private final boolean withExtension;

    public TestApplication(boolean withExtension) {
      this.withExtension = withExtension;
    }

    @Override
    public void initialize(Bootstrap<TestConfiguration> bootstrap) {
      OpaBundle<TestConfiguration> bundle;
      if (withExtension) {
        bundle =
            OpaBundle.builder()
                .withOpaConfigProvider(TestConfiguration::getOpa)
                .withInputExtension("body", new BodyInputExtension(bootstrap.getObjectMapper()))
                .build();
      } else {
        bundle = OpaBundle.builder().withOpaConfigProvider(TestConfiguration::getOpa).build();
      }

      bootstrap.addBundle(bundle);
    }

    @Override
    public void run(TestConfiguration configuration, Environment environment) {
      environment.jersey().register(Endpoint.class);
    }
  }

  @Path("")
  public static class Endpoint {
    @Path("/")
    @POST
    public String ping(Map<String, String> input) {
      // throws NullPointerException since body has already been consumed in the BodyInputExtension
      if (input == null) {
        throw new BadRequestException("Received null input.");
      }

      return input.get("key");
    }
  }

  public static class TestConfiguration extends Configuration {
    private OpaConfig opa;

    public OpaConfig getOpa() {
      return opa;
    }
  }
}
