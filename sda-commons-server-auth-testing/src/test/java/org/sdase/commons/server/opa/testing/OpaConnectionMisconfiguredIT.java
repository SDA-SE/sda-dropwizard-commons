package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.junit5.DropwizardAppExtension;

public class OpaConnectionMisconfiguredIT {

  @RegisterExtension
  public static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class, resourceFilePath("test-opa-config.yaml"));

  @Test
  void shouldDenyAccess() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get(); // NOSONAR

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }
}
