package org.sdase.commons.server.swagger;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.Invocation;
import org.junit.Rule;
import org.junit.Test;

/**
 * This test suite checks if the correct swagger file is generated if multiple different apps are
 * executed in the same JVM.
 */
public class SwaggerBundleMultipleApplicationsIT {
  @Rule
  public final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(SwaggerBundleTestApp.class, resourceFilePath("test-config.yaml"));

  @Test
  public void shouldIncludeInfoTry1() {
    shouldIncludeInfoBase();
  }

  @Test
  public void shouldIncludeInfoTry2() {
    shouldIncludeInfoBase();
  }

  private void shouldIncludeInfoBase() {
    String response = getJsonRequest().get(String.class);

    assertThatJson(response)
        .inPath("$.info.title")
        .isEqualTo(((SwaggerBundleTestApp) DW.getApplication()).getTitle());
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1");
  }

  private Invocation.Builder getJsonRequest() {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path("/swagger.json")
        .request(APPLICATION_JSON);
  }
}
