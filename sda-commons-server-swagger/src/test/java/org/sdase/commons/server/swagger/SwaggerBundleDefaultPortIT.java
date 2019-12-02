package org.sdase.commons.server.swagger;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sdase.commons.server.swagger.test.SwaggerAssertions;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

@Ignore("This test can not be run on the build server since it binds to port 80 which is not permitted (Permission denied)")
public class SwaggerBundleDefaultPortIT {
   @ClassRule
   public static final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(
         SwaggerBundleTestApp.class,
         resourceFilePath("test-config.yaml"),
         ConfigOverride.config("server.applicationConnectors[0].port", "80"));

   private static Builder getJsonRequest() {
      return DW.client()
            .target(getTarget())
            .path("/swagger.json")
            .request(APPLICATION_JSON);
   }

   private static String getTarget() {
      return "http://localhost:" + DW.getLocalPort();
   }

   @Test
   public void shouldProvideSchemaCompliantJson() {
      try (Response response = getJsonRequest().get()) {

         assertThat(response.getStatus()).isEqualTo(OK_200);
         assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);

         SwaggerAssertions.assertValidSwagger2Json(response);
      }
   }

   @Test
   public void shouldDeduceHost() {
      String response = getJsonRequest().get(String.class);

      assertThatJson(response)
          .inPath("$.host")
          .asString()
          .contains("localhost")
          .doesNotContain(":-1");
   }
}
