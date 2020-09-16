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
import org.sdase.commons.server.openapi.apps.file.FromFileTestApp;
import org.sdase.commons.server.openapi.test.OpenApiAssertions;

public class OpenApiBundleFileIT {
  private static final String HOUSE_DEFINITION = "House";

  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(FromFileTestApp.class, resourceFilePath("test-config.yaml"));

  private static Builder getJsonRequest() {
    return DW.client()
        .target(getTarget())
        .path("api")
        .path("openapi.json")
        .request(APPLICATION_JSON);
  }

  private static Builder getYamlRequest() {
    return DW.client()
        .target(getTarget())
        .path("api")
        .path("openapi.yaml")
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
            .path("house")
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

    assertThatJson(response).inPath("$.info.title").isEqualTo("A manually written OpenAPI file");
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1.1");
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

    assertThatJson(response)
        .inPath("$.paths")
        .isObject()
        .containsOnlyKeys("/house", "/embed", "/embedAllOf", "/embedAnyOf");

    assertThatJson(response).inPath("$.paths./house").isObject().containsOnlyKeys("get", "put");
  }

  @Test
  public void shouldIncludeSchemas() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas")
        .isObject()
        .containsKeys(HOUSE_DEFINITION);

    assertThatJson(response)
        .inPath("$.components.schemas." + HOUSE_DEFINITION + ".properties")
        .isObject()
        .containsOnlyKeys("_embedded", "_links");
  }

  @Test
  public void shouldUseDescriptionFromAnnotation() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas." + HOUSE_DEFINITION + ".description")
        .isAbsent();
  }

  @Test
  public void shouldNotIncludeAdditionalReturnCode() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas." + HOUSE_DEFINITION + ".responses.500")
        .isAbsent();
  }

  @Test
  public void shouldIncludeEmbedParameterExistingEmbeddedProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths./embed.get.parameters[0].schema.items.enum")
        .isArray()
        .containsOnly("one", "two");
  }

  @Test
  public void shouldIncludeEmbedParameterExistingEmbeddedAllOfProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths./embedAllOf.get.parameters[0].schema.items.enum")
        .isArray()
        .containsOnly("three", "four");
  }

  @Test
  public void shouldNotIncludeEmbedParameterExistingEmbeddedAnyOfProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.paths./embedAnyOf.get.parameters").isAbsent();
  }
}
