package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

class OpaDisabledIT {

  @RegisterExtension
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-opa-config.yaml"),
          config("opa.disableOpa", "true"));

  @Test
  @RetryingTest(5)
  void shouldAllowAccess() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get()) {

      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    } // NOSONAR
  }

  @Test
  @RetryingTest(5)
  void shouldNotIncludeHealthCheck() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort()) // NOSONAR
            .path("healthcheck")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.readEntity(String.class))
        .doesNotContain(PolicyExistsHealthCheck.DEFAULT_NAME);
  }
}
