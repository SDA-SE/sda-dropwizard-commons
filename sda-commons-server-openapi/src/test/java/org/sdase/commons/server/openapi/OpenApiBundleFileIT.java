package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.openapi.apps.file.FromFileTestApp;
import org.sdase.commons.server.openapi.test.OpenApiAssertions;

class OpenApiBundleFileIT {
  private static final String HOUSE_DEFINITION = "House";

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(FromFileTestApp.class, resourceFilePath("test-config.yaml"));

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
  @RetryingTest(5)
  void shouldProvideSchemaCompliantJson() {
    try (Response response = getJsonRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);

      OpenApiAssertions.assertValid(response);
    }
  }

  @Test
  @RetryingTest(5)
  void shouldProvideValidYaml() {
    try (Response response = getYamlRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/yaml"));

      OpenApiAssertions.assertValid(response);
    }
  }

  @Test
  @RetryingTest(5)
  void shouldProvideYamlInUtf8() {
    try (Response response = getYamlRequest().get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/yaml"));

      byte[] bytes = response.readEntity(byte[].class);
      String content = new String(bytes, StandardCharsets.UTF_8);

      assertThat(content).contains("\u00f6");
    }
  }

  @Test
  @RetryingTest(5)
  void shouldHaveCORSWildcardJson() {
    try (Response response = getJsonRequest().header("Origin", "example.com").get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
    }
  }

  @Test
  @RetryingTest(5)
  void shouldHaveCORSWildcardYaml() {
    try (Response response = getYamlRequest().header("Origin", "example.com").get()) {
      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
    }
  }

  @Test
  @RetryingTest(5)
  void shouldNotHaveCORSWildcardOnOtherPath() {
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
  @RetryingTest(5)
  void shouldIncludeInfo() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.info.title").isEqualTo("A manually written OpenAPI file");
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1.1");
  }

  @Test
  @RetryingTest(5)
  void shouldIncludeServerUrl() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.servers[*].url")
        .isArray()
        .containsExactly(String.format("http://localhost:%s/api/", DW.getLocalPort()));
  }

  @Test
  @RetryingTest(5)
  void shouldIncludePaths() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths")
        .isObject()
        .containsOnlyKeys("/house", "/embed", "/embedAllOf", "/embedAnyOf");

    assertThatJson(response).inPath("$.paths./house").isObject().containsOnlyKeys("get", "put");
  }

  @Test
  @RetryingTest(5)
  void shouldIncludeSchemas() {
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
  @RetryingTest(5)
  void shouldUseDescriptionFromAnnotation() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas." + HOUSE_DEFINITION + ".description")
        .isAbsent();
  }

  @Test
  @RetryingTest(5)
  void shouldNotIncludeAdditionalReturnCode() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.components.schemas." + HOUSE_DEFINITION + ".responses.500")
        .isAbsent();
  }

  @Test
  @RetryingTest(5)
  void shouldIncludeEmbedParameterExistingEmbeddedProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths./embed.get.parameters[0].schema.items.enum")
        .isArray()
        .containsOnly("one", "two");
  }

  @Test
  @RetryingTest(5)
  void shouldIncludeEmbedParameterExistingEmbeddedAllOfProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.paths./embedAllOf.get.parameters[0].schema.items.enum")
        .isArray()
        .containsOnly("three", "four");
  }

  @Test
  @RetryingTest(5)
  void shouldNotIncludeEmbedParameterExistingEmbeddedAnyOfProperty() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response).inPath("$.paths./embedAnyOf.get.parameters").isAbsent();
  }
}
