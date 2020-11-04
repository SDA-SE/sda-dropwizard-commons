package org.sdase.commons.server.opa.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.DropwizardRuleHelper;

public class OpaConnectionMisconfiguredIT {

  @ClassRule
  public static final DropwizardAppRule<OpaBundeTestAppConfiguration> DW =
      DropwizardRuleHelper.dropwizardTestAppFrom(OpaBundleTestApp.class)
          .withConfigFrom(OpaBundeTestAppConfiguration::new)
          .withRandomPorts()
          .build();

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
