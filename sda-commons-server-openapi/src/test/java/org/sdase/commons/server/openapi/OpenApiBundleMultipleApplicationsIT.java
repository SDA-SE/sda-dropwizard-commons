package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.Invocation;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.openapi.apps.alternate.AnotherTestApp;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;

/**
 * This test suite checks if the correct swagger file is generated if multiple different apps are
 * executed in the same JVM.
 */
public class OpenApiBundleMultipleApplicationsIT {
  public DropwizardAppRule<Configuration> DW;

  @Test
  public void shouldIncludeInfoTry1() throws Throwable {
    DW = new DropwizardAppRule<>(OpenApiBundleTestApp.class, resourceFilePath("test-config.yaml"));

    DW.apply(
            new Statement() {
              @Override
              public void evaluate() {
                String response = getJsonRequest().get(String.class);

                assertThatJson(response).inPath("$.info.title").isEqualTo("A test app");
                assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1");
              }
            },
            Description.EMPTY)
        .evaluate();
  }

  @Test
  public void shouldIncludeInfoTry2() throws Throwable {
    DW = new DropwizardAppRule<>(AnotherTestApp.class, resourceFilePath("test-config.yaml"));

    DW.apply(
            new Statement() {
              @Override
              public void evaluate() {
                String response = getJsonRequest().get(String.class);

                assertThatJson(response).inPath("$.info.title").isEqualTo("Another test app");
                assertThatJson(response).inPath("$.info.version").asString().isEqualTo("2");
              }
            },
            Description.EMPTY)
        .evaluate();
  }

  private Invocation.Builder getJsonRequest() {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path("/api")
        .path("/openapi.json")
        .request(APPLICATION_JSON);
  }
}
