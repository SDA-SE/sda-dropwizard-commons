package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.OK_200;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;
import org.sdase.commons.server.openapi.test.OpenApiAssertions;

@Ignore(
    "This test can not be run on the build server since it binds to port 80 which is not permitted (Permission denied)")
public class OpenApiBundleDefaultPortIT {
  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          OpenApiBundleTestApp.class,
          resourceFilePath("test-config.yaml"),
          ConfigOverride.config("server.applicationConnectors[0].port", "80"),
          ConfigOverride.config("server.rootPath", "/"));

  private static Builder getJsonRequest() {
    return DW.client().target(getTarget()).path("/openapi.json").request(APPLICATION_JSON);
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
  public void shouldDeduceHost() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.servers[*].url")
        .isArray()
        .containsExactly("http://localhost/");
  }
}
