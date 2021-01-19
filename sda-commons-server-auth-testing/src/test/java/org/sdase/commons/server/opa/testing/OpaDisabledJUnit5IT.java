package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

public class OpaDisabledJUnit5IT {

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
          config("opa.disableOpa", "true"));

  @RetryingTest(5)
  public void shouldAllowAccess() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get(); // NOSONAR

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
  }

  @RetryingTest(5)
  public void shouldNotIncludeHealthCheck() {
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
