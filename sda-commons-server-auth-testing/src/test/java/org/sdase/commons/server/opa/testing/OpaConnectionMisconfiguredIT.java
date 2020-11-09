package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;

public class OpaConnectionMisconfiguredIT {

  @ClassRule
  public static final DropwizardAppRule<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppRule<>(OpaBundleTestApp.class, resourceFilePath("test-opa-config.yaml"));

  @Test
  public void shouldDenyAccess() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get(); // NOSONAR

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }
}
