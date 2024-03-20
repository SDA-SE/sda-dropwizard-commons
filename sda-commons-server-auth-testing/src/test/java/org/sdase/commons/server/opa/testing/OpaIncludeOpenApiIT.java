package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

class OpaIncludeOpenApiIT {

  @RegisterExtension
  @Order(0)
  static final OpaClassExtension OPA_EXTENSION = new OpaClassExtension();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-opa-config.yaml"),
          config("opa.baseUrl", OPA_EXTENSION::getUrl),
          config("opa.excludeOpenApi", "false"));

  private static final String path = "resources";
  private static final String method = "GET";

  @BeforeEach
  void before() {
    OPA_EXTENSION.reset();
  }

  @Test
  @RetryingTest(5)
  void shouldInvokeOpenApiUrls() {
    // given
    String includePath = "openapi.json";
    OPA_EXTENSION.mock(onRequest().withHttpMethod("GET").withPath(includePath).allow());
    // when
    try (Response response = doGetRequest(includePath)) {
      // then
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      OPA_EXTENSION.verify(1, "GET", includePath);
    }
  }

  private Response doGetRequest(String rPath) {
    return DW.client().target("http://localhost:" + DW.getLocalPort()).path(rPath).request().get();
  }

  private Response doPostRequest(String rPath) {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path(rPath)
        .request()
        .post(Entity.json(null));
  }
}
