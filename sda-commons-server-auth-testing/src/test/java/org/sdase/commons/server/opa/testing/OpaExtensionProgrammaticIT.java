package org.sdase.commons.server.opa.testing;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;
import org.sdase.commons.server.testing.junit5.DropwizardAppExtension;

import javax.ws.rs.core.Response;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaExtension.onRequest;

public class OpaExtensionProgrammaticIT {

  @RegisterExtension
  @Order(0)
  static final OpaExtension OPA_EXTENSION = new OpaExtension();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-opa-config.yaml"),
          config("opa.baseUrl", OPA_EXTENSION::getUrl)
      );

  // only one test since this is for demonstration with programmatic config
  @RetryingTest(5)
  void shouldAllowAccess() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod("GET").withPath("resources").allow());

    // when
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get(); // NOSONAR

    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
    assertThat(principalInfo.getConstraints().getConstraint()).isNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
    assertThat(principalInfo.getJwt()).isNull();
  }
}
