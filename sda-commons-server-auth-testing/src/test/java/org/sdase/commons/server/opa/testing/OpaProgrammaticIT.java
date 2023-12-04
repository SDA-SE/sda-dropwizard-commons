package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.Response;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;

class OpaProgrammaticIT {

  @RegisterExtension
  @Order(0)
  static final OpaClassExtension OPA_EXTENSION = new OpaClassExtension();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA_EXTENSION::getUrl));

  // only one test since this is for demonstration with programmatic config
  @Test
  @RetryingTest(5)
  void shouldAllowAccess() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod("GET").withPath("resources").allow());

    // when
    PrincipalInfo principalInfo;
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get()) {

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
      principalInfo = response.readEntity(PrincipalInfo.class);
    } // NOSONAR
    assertThat(principalInfo.getConstraints().getConstraint()).isNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
    assertThat(principalInfo.getJwt()).isNull();
  }
}
