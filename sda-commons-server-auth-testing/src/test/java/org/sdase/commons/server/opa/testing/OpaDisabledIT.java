package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.junit5.DropwizardAppExtension;

public class OpaDisabledIT {

  @RegisterExtension
  public static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-opa-config.yaml"),
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
