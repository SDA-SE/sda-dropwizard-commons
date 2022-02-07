package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;
import org.sdase.commons.server.openapi.test.OpenApiAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OpenApiBundleIT {

  private static final String NATURAL_PERSON_DEFINITION = "NaturalPerson";
  private static final String PARTNER_DEFINITION = "Partner";
  private static final String HOUSE_DEFINITION = "House";

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiBundleIT.class);

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          OpenApiBundleTestApp.class, resourceFilePath("test-config.yaml"));

  private static Builder getJsonRequest() {
    return DW.client()
        .target(getTarget())
        .path("/api")
        .path("/openapi.json")
        .request(APPLICATION_JSON);
  }

  private static Builder getYamlRequest() {
    return DW.client()
        .target(getTarget())
        .path("/api")
        .path("/openapi.yaml")
        .request("application/yaml");
  }

  private static String getTarget() {
    return "http://localhost:" + DW.getLocalPort();
  }

  @Test
  void shouldProvideSchemaCompliantJson() {
    try (Response response = getJsonRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);

      OpenApiAssertions.assertValid(response);
    }
  }

  @Test
  void shouldProvideValidYaml() {
    try (Response response = getYamlRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/yaml"));

      OpenApiAssertions.assertValid(response);
    }
  }

  @Test
  void shouldHaveCORSWildcardJson() {
    try (Response response = getJsonRequest().header("Origin", "example.com").get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
    }
  }

  @Test
  void shouldHaveCORSWildcardYaml() {
    try (Response response = getYamlRequest().header("Origin", "example.com").get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
    }
  }

  @Test
  void shouldNotHaveCORSWildcardOnOtherPath() {
    try (Response response =
        DW.client()
            .target(getTarget())
            .path("api")
            .path("jdoe")
            .request()
            .header("Origin", "example.com")
            .get()) {

      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isNull();
    }
  }

  @Test
  void shouldIncludeInfo() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.info.title").isEqualTo("A test app");
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1");
  }

  @Test
  void shouldIncludeServerUrl() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.servers[*].url")
        .isArray()
        .containsExactly(String.format("http://localhost:%s/api/", DW.getLocalPort()));
  }

  @Test
  void shouldIncludePaths() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths")
        .isObject()
        .containsOnlyKeys("/jdoe", "/house", "/houses", "/partners");

    assertThatJson(response)
        .inPath("$.paths./jdoe")
        .isObject()
        .containsOnlyKeys("get", "post", "delete");

    assertThatJson(response).inPath("$.paths./house").isObject().containsOnlyKeys("get");

    assertThatJson(response).inPath("$.paths./houses").isObject().containsOnlyKeys("get");
  }

  @Test
  @StdIo
  void shouldIncludeSchemas(StdOut stdOut) {
    String response = getJsonRequest().get(String.class);

    String output = String.join("\n", stdOut.capturedLines());
    assertThat(output).doesNotContain("java.lang.NullPointerException");
    LOGGER.info(output);

    assertThatJson(response)
        .inPath("$.components.schemas")
        .isObject()
        .containsOnlyKeys(
            NATURAL_PERSON_DEFINITION,
            PARTNER_DEFINITION,
            "HALLink",
            "Animal",
            "House",
            "HouseSearchResource",
            "PartnerSearchResultResource");

    assertThatJson(response)
        .inPath("$.components.schemas." + PARTNER_DEFINITION + ".properties") // NOSONAR
        .isObject()
        .containsOnlyKeys("type", "_embedded");

    assertThatJson(response)
        .inPath("$.components.schemas." + NATURAL_PERSON_DEFINITION + ".allOf[1].properties")
        .isObject()
        .containsOnlyKeys("firstName", "lastName", "traits", "_links", "_embedded");
  }

  @Test
  void shouldIncludePropertyExampleAsJson() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath(
            "$.components.schemas."
                + NATURAL_PERSON_DEFINITION
                + ".allOf[1].properties.traits.example")
        .isArray()
        .containsExactlyInAnyOrder("hipster", "generous");

    assertThatJson(response)
        .inPath("$.components.schemas." + PARTNER_DEFINITION + ".properties.type.example")
        .isString()
        .isEqualTo("naturalPerson");
  }

  @Test
  void shouldIncludeHALSelfLink() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath(
            "$.components.schemas."
                + NATURAL_PERSON_DEFINITION
                + ".allOf[1].properties._links.properties")
        .isObject()
        .containsOnlyKeys("self");

    assertThatJson(response)
        .inPath("$.components.schemas." + HOUSE_DEFINITION + ".properties._links.properties")
        .isObject()
        .containsOnlyKeys("self", "partners", "animals");
  }

  @Test
  void shouldNotUsePropertyDescriptionAsComponentDescription() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.components.schemas.Partner.description").isAbsent();
  }

  @Test
  void shouldHaveCustomDescriptionInHALLink() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas.HALLink.description")
        .isEqualTo("Representation of a link as defined in HAL");
  }

  @Test
  void shouldIncludeEmbedParameter() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.paths./house.get.parameters").isArray().isNotEmpty();

    assertThatJson(response)
        .inPath("$.paths./house.get.parameters[0].schema.items.enum")
        .isArray()
        .containsOnly("animals", "partners");
  }

  @Test
  void shouldIncludeEmbedParameterForNestedProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths./houses.get.parameters[0].schema.items.enum")
        .isArray()
        .containsOnly("animals", "partners");
  }
}
