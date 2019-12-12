package org.sdase.commons.server.opa.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaRule.onRequest;

import io.dropwizard.testing.junit.DropwizardAppRule;

import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaProgrammaticIT {


  private static final OpaRule OPA_RULE = new OpaRule();

  private static final LazyRule<DropwizardAppRule<OpaBundeTestAppConfiguration>> DW = new LazyRule<>( () ->
       DropwizardRuleHelper.dropwizardTestAppFrom(OpaBundleTestApp.class)
           .withConfigFrom(OpaBundeTestAppConfiguration::new)
      .withRandomPorts()
      .withConfigurationModifier(c -> c.getOpa().setBaseUrl(OPA_RULE.getUrl())).build());

  @ClassRule
  public static final RuleChain chain = RuleChain.outerRule(OPA_RULE).around(DW);
  @Rule
  public RetryRule rule = new RetryRule();

  // only one test since this is for demonstration with programmatic config
  @Test
  @Retry(5)
  public void shouldAllowAccess() {
    // given
    OPA_RULE.mock(onRequest("GET", "resources").allow());

    // when
    Response response = DW.getRule().client().target("http://localhost:" + DW.getRule().getLocalPort()) // NOSONAR
        .path("resources").request().get(); // NOSONAR

      // then
      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getConstraints().getConstraint()).isNull();
      assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
      assertThat(principalInfo.getJwt()).isNull();
   }
}
