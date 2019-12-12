package org.sdase.commons.server.opa.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaDisabledIT {

   @ClassRule
   public static final LazyRule<DropwizardAppRule<OpaBundeTestAppConfiguration>> DW = new LazyRule<>(
         () -> DropwizardRuleHelper
               .dropwizardTestAppFrom(OpaBundleTestApp.class)
               .withConfigFrom(OpaBundeTestAppConfiguration::new)
               .withRandomPorts()
               .withConfigurationModifier(c -> c.getOpa().setDisableOpa(true))
               .build());

   @Rule
   public RetryRule rule = new RetryRule();

   @Test
   @Retry(5)
   public void shouldAllowAccess() {
      Response response = DW
            .getRule()
            .client()
            .target("http://localhost:" + DW.getRule().getLocalPort()) // NOSONAR
            .path("resources")
            .request()
            .get(); // NOSONAR

      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
   }

  @Test
  @Retry(5)
  public void shouldNotIncludeHealthCheck() {
    Response response = DW
        .getRule()
        .client()
        .target("http://localhost:" + DW.getRule().getAdminPort()) // NOSONAR
        .path("healthcheck")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get();

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.readEntity(String.class)).doesNotContain(PolicyExistsHealthCheck.DEFAULT_NAME);

  }

}
