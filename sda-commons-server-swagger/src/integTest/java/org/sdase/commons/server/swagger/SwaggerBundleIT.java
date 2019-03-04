package org.sdase.commons.server.swagger;

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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.swagger.test.SwaggerAssertions;

public class SwaggerBundleIT {

   private static final String NATURAL_PERSON_DEFINITION = "NaturalPerson";
   private static final String PARTNER_DEFINITION = "Partner";

   @ClassRule
   public static final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(
         SwaggerBundleTestApp.class, resourceFilePath("test-config.yaml"));

   private static Builder getJsonRequest() {
      return DW.client()
            .target(getTarget())
            .path("/swagger.json")
            .request(APPLICATION_JSON);
   }

   private static Builder getYamlRequest() {
      return DW.client()
            .target(getTarget())
            .path("/swagger.yaml")
            .request("application/yaml");
   }

   private static String getTarget() {
      return "http://localhost:" + DW.getLocalPort();
   }

   @BeforeClass
   public static void setup() {
      // allow to set headers in jersey client
      System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
   }

   @Test
   public void shouldProvideSchemaCompliantJson() {
      Response response = getJsonRequest().get();

      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);

      SwaggerAssertions.assertValidSwagger2Json(response);
   }

   @Test
   public void shouldProvideValidYaml() {
      Response response = getYamlRequest().get();

      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getMediaType()).isEqualTo(MediaType.valueOf("application/yaml"));

      SwaggerAssertions.assertValidSwagger2Yaml(response);
   }

   @Test
   public void shouldHaveCORSWildcardJson() {
      Response response = getJsonRequest().header("Origin", "example.com").get();

      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
   }

   @Test
   public void shouldHaveCORSWildcardYaml() {
      Response response = getYamlRequest().header("Origin", "example.com").get();

      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isEqualTo("example.com");
   }

   @Test
   public void shouldNotHaveCORSWildcardOnOtherPath() {
      Response response = DW.client()
          .target(getTarget())
          .path("jdoe")
          .request()
          .header("Origin", "example.com").get();

      assertThat(response.getStatus()).isEqualTo(OK_200);
      assertThat(response.getHeaderString("Access-Control-Allow-Origin")).isNull();
   }

   @Test
   public void shouldIncludeInfo() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.info.title")
          .isEqualTo(SwaggerBundleTestApp.class.getSimpleName());
      assertThatJson(response)
          .inPath("$.info.version")
          .asString()
          .isEqualTo("1");
   }

   @Test
   public void shouldIncludeBasePath() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.basePath")
          .asString()
          .isEqualTo("/");
   }

   @Test
   public void shouldDeduceHost() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.host")
          .asString()
          .contains("localhost:" + DW.getLocalPort());
   }

   @Test
   public void shouldIncludePaths() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.paths")
          .isObject()
          .containsOnlyKeys("/jdoe");

      assertThatJson(response)
          .inPath("$.paths./jdoe")
          .isObject()
          .containsOnlyKeys("get", "post", "delete");
   }

   @Test
   public void shouldIncludeDefinitions() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.definitions")
          .isObject()
          .containsKeys(NATURAL_PERSON_DEFINITION, PARTNER_DEFINITION);

      assertThatJson(response)
          .inPath("$.definitions." + PARTNER_DEFINITION + ".properties") // NOSONAR
          .isObject()
          .containsKeys("type");

      assertThatJson(response)
          .inPath("$.definitions." + NATURAL_PERSON_DEFINITION + ".allOf[1].properties")
          .isObject()
          .containsKeys("firstName", "lastName", "traits", "_links");
   }

   @Test
   public void shouldIncludePropertyExampleAsJson() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.definitions." + NATURAL_PERSON_DEFINITION + ".allOf[1].properties.traits.example")
          .isArray()
          .containsExactlyInAnyOrder("hipster", "generous");

      assertThatJson(response)
          .inPath("$.definitions." + PARTNER_DEFINITION + ".properties.type.example")
          .isString()
          .isEqualTo("naturalPerson");
   }

   @Test
   public void shouldIncludeHALSelfLink() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.definitions." + NATURAL_PERSON_DEFINITION + ".allOf[1].properties._links.properties")
          .isObject()
          .containsKeys("self");
   }
}
