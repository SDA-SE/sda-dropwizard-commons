package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;
import org.sdase.commons.server.openapi.test.OpenApiAssertions;

public class OpenApiBundleIT {

  private static final String NATURAL_PERSON_DEFINITION = "NaturalPerson";
  private static final String PARTNER_DEFINITION = "Partner";

  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(OpenApiBundleTestApp.class, resourceFilePath("test-config.yaml"));

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
  public void shouldProvideSchemaCompliantJson() {
    try (Response response = getJsonRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);

      OpenApiAssertions.assertValid(response);
    }
  }

  @Test
  public void shouldProvideValidYaml() {
    try (Response response = getYamlRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/yaml"));

      OpenApiAssertions.assertValid(response);
    }
  }

  @Test
  public void shouldHaveCORSWildcardJson() {
    try (Response response = getJsonRequest().header("Origin", "example.com").get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
    }
  }

  @Test
  public void shouldHaveCORSWildcardYaml() {
    try (Response response = getYamlRequest().header("Origin", "example.com").get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
    }
  }

  @Test
  public void shouldNotHaveCORSWildcardOnOtherPath() {
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
  public void shouldIncludeInfo() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.info.title").isEqualTo("A test app");
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1");
  }

  @Test
  public void shouldIncludeServerUrl() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.servers[*].url")
        .isArray()
        .containsExactly(String.format("http://localhost:%s/api/", DW.getLocalPort()));
  }

  @Test
  public void shouldIncludePaths() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.paths").isObject().containsOnlyKeys("/jdoe", "/house");

    assertThatJson(response)
        .inPath("$.paths./jdoe")
        .isObject()
        .containsOnlyKeys("get", "post", "delete");

    assertThatJson(response).inPath("$.paths./house").isObject().containsOnlyKeys("get");
  }

  @Test
  public void shouldIncludeSchemas() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas")
        .isObject()
        .containsOnlyKeys(NATURAL_PERSON_DEFINITION, PARTNER_DEFINITION, "Animal", "House");

    assertThatJson(response)
        .inPath("$.components.schemas." + PARTNER_DEFINITION + ".properties") // NOSONAR
        .isObject()
        .containsOnlyKeys("type", "options");

    assertThatJson(response)
        .inPath("$.components.schemas." + NATURAL_PERSON_DEFINITION + ".allOf[1].properties")
        .isObject()
        .containsOnlyKeys("firstName", "lastName", "traits");
  }

  @Test
  public void shouldIncludePropertyExampleAsJson() {
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
}
