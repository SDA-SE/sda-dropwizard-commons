package org.sdase.commons.server.opa.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.util.Lists.newArrayList;
import static org.sdase.commons.server.opa.testing.OpaRule.onRequest;

import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.testing.AuthRule;
import org.sdase.commons.server.opa.testing.test.AuthAndOpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.AuthAndOpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

public class AuthAndOpaIT {

   private static final AuthRule AUTH = AuthRule.builder().build();

   private static final OpaRule OPA_RULE = new OpaRule();

   private static final LazyRule<DropwizardAppRule<AuthAndOpaBundeTestAppConfiguration>> DW = new LazyRule<>(
         () -> DropwizardRuleHelper
               .dropwizardTestAppFrom(AuthAndOpaBundleTestApp.class)
               .withConfigFrom(AuthAndOpaBundeTestAppConfiguration::new)
               .withRandomPorts()
               .withConfigurationModifier(AUTH.applyConfig(AuthAndOpaBundeTestAppConfiguration::setAuth))
               .withConfigurationModifier(c -> c.getOpa().setBaseUrl(OPA_RULE.getUrl()))
               .build());

   @ClassRule
   public static final RuleChain chain = RuleChain.outerRule(OPA_RULE).around(DW);

   private static String path = "resources";
   private static String method = "GET";
   private String jwt;

   @Before
   public void before() {
      jwt = AUTH.auth().buildToken();
      OPA_RULE.reset();
   }

   @Test
   public void shouldAllowAccessSimple() {
      OPA_RULE.mock(onRequest(method, path).allow());

      Response response = getResources();

      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getJwt()).isEqualTo(jwt);
      assertThat(principalInfo.getConstraints()).isNull();
   }

   @Test
   public void shouldAllowAccessConstraints() {
      OPA_RULE.mock(onRequest(method, path).allow().withConstraint(new ConstraintModel().setFullAccess(true).addConstraint("customer_ids", "1")));

      Response response = getResources();

      assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
      PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
      assertThat(principalInfo.getJwt()).isNotNull();
      assertThat(principalInfo.getConstraints().isFullAccess()).isTrue();
      assertThat(principalInfo.getConstraints().getConstraint()).contains(entry("customer_ids", newArrayList("1")));
   }



   private Response getResources() {
      MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
      headers.put(HttpHeaders.AUTHORIZATION, newArrayList("Bearer " + jwt));
      return DW
            .getRule()
            .client()
            .target("http://localhost:" + DW.getRule().getLocalPort()) // NOSONAR
            .path(path)
            .request()
            .headers(headers)
            .get();
   }

}
