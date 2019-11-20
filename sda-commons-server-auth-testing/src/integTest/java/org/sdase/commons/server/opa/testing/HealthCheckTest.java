package org.sdase.commons.server.opa.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;

public class HealthCheckTest {

   @ClassRule
   public static final OpaRule OPA_RULE = new OpaRule();

   private PolicyExistsHealthCheck policyExistsHealthCheck;

   @Before
   public void before() {
      OPA_RULE.reset();
      WebTarget target = JerseyClientBuilder.createClient().target(OPA_RULE.getUrl());
      policyExistsHealthCheck = new PolicyExistsHealthCheck(target);
   }

   @Test
   public void shouldBeHealthyIfNormalResponse() {
      // since the health check does not send any input, the response of OPA
      // will be false (default) for allow
      OPA_RULE.mock(onAnyRequest().deny());
      assertThat(policyExistsHealthCheck.check().isHealthy()).isTrue();
   }

   @Test
   public void shouldBeUnhealthyIfOpaGivesEmptyResponse() {
      OPA_RULE.mock(onAnyRequest().emptyResponse());
      assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
   }

   @Test
   public void shouldBeUnhealthyIfOpaError() {
      OPA_RULE.mock(onAnyRequest().serverError());
      assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
   }

   @Test
   public void shouldBeUnhealthyIfMessageDoesNotContainDefaultDecision() {
      OPA_RULE.mock(onAnyRequest().answer(new OpaResponse()));
      assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
   }
}
