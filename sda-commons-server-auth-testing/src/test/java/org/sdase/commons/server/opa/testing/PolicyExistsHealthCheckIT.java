package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;

import com.codahale.metrics.health.HealthCheck.Result;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class PolicyExistsHealthCheckIT {

  @ClassRule public static final OpaRule OPA_RULE = new OpaRule();
  @Rule public RetryRule rule = new RetryRule();

  private PolicyExistsHealthCheck policyExistsHealthCheck;

  @Before
  public void before() {
    OPA_RULE.reset();
    WebTarget target = JerseyClientBuilder.createClient().target(OPA_RULE.getUrl());
    policyExistsHealthCheck = new PolicyExistsHealthCheck(target);
  }

  @Test
  @Retry(5)
  public void shouldBeHealthyIfNormalResponse() {
    // since the health check does not send any input, the response of OPA
    // will be false (default) for allow
    OPA_RULE.mock(onAnyRequest().deny());
    assertThat(policyExistsHealthCheck.check().isHealthy()).isTrue();
  }

  @Test
  @Retry(5)
  public void shouldBeUnhealthyIfOpaGivesEmptyResponse() {
    OPA_RULE.mock(onAnyRequest().emptyResponse());
    assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
  }

  @Test
  @Retry(5)
  public void shouldBeUnhealthyIfOpaGivesNullResponse() {
    OPA_RULE.mock(wire -> wire.stubFor(post(anyUrl()).willReturn(okJson("{}"))));
    final Result check = policyExistsHealthCheck.check();
    assertThat(check.isHealthy()).isFalse();
    assertThat(check.getMessage())
        .isEqualTo("The policy response seems not to be SDA guideline compliant");
  }

  @Test
  @Retry(5)
  public void shouldBeUnhealthyIfOpaError() {
    OPA_RULE.mock(onAnyRequest().serverError());
    assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
  }

  @Test
  @Retry(5)
  public void shouldBeUnhealthyIfMessageDoesNotContainDefaultDecision() {
    OPA_RULE.mock(onAnyRequest().answer(new OpaResponse()));
    assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
  }
}
