package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Invocation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.openapi.apps.alternate.AnotherTestApp;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;

/**
 * This test suite checks if the correct swagger file is generated if multiple different apps are
 * executed in the same JVM.
 */
class OpenApiBundleMultipleApplicationsIT {

  @RegisterExtension
  @Order(0)
  private static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          OpenApiBundleTestApp.class, resourceFilePath("test-config.yaml"));

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<Configuration> DW_ANOTHER_APP =
      new DropwizardAppExtension<>(AnotherTestApp.class, resourceFilePath("test-config.yaml"));

  @Test
  void shouldIncludeInfoTry1() {
    String response = getJsonRequest(DW).get(String.class);

    assertThatJson(response).inPath("$.info.title").isEqualTo("A test app");
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("1");
  }

  @Test
  void shouldIncludeInfoTry2() {
    String response = getJsonRequest(DW_ANOTHER_APP).get(String.class);

    assertThatJson(response).inPath("$.info.title").isEqualTo("Another test app");
    assertThatJson(response).inPath("$.info.version").asString().isEqualTo("2");
  }

  private Invocation.Builder getJsonRequest(DropwizardAppExtension<Configuration> DW) {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path("/api")
        .path("/openapi.json")
        .request(APPLICATION_JSON);
  }
}
