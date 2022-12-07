package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onAnyRequest;

import com.codahale.metrics.health.HealthCheck.Result;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;

class PolicyExistsHealthCheckIT {

  @RegisterExtension private static final OpaClassExtension OPA_EXTENSION = new OpaClassExtension();

  private PolicyExistsHealthCheck policyExistsHealthCheck;

  @BeforeEach
  void before() {
    OPA_EXTENSION.reset();
    WebTarget target = JerseyClientBuilder.createClient().target(OPA_EXTENSION.getUrl());
    policyExistsHealthCheck = new PolicyExistsHealthCheck(target);
  }

  @Test
  @RetryingTest(5)
  void shouldBeHealthyIfNormalResponse() {
    // since the health check does not send any input, the response of OPA
    // will be false (default) for allow
    OPA_EXTENSION.mock(onAnyRequest().deny());
    assertThat(policyExistsHealthCheck.check().isHealthy()).isTrue();
  }

  @Test
  @RetryingTest(5)
  void shouldBeUnhealthyIfOpaGivesEmptyResponse() {
    OPA_EXTENSION.mock(onAnyRequest().emptyResponse());
    assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
  }

  @Test
  @RetryingTest(5)
  void shouldBeUnhealthyIfOpaGivesNullResponse() {
    OPA_EXTENSION.mock(wire -> wire.stubFor(post(anyUrl()).willReturn(okJson("{}"))));
    final Result check = policyExistsHealthCheck.check();
    assertThat(check.isHealthy()).isFalse();
    assertThat(check.getMessage())
        .isEqualTo("The policy response seems not to be SDA guideline compliant");
  }

  @Test
  @RetryingTest(5)
  void shouldBeUnhealthyIfOpaError() {
    OPA_EXTENSION.mock(onAnyRequest().serverError());
    assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
  }

  @Test
  @RetryingTest(5)
  void shouldBeUnhealthyIfMessageDoesNotContainDefaultDecision() {
    OPA_EXTENSION.mock(onAnyRequest().answer(new OpaResponse()));
    assertThat(policyExistsHealthCheck.check().isHealthy()).isFalse();
  }
}
