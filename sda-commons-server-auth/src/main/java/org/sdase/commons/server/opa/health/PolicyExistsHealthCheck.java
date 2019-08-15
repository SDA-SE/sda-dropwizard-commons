package org.sdase.commons.server.opa.health;

import com.codahale.metrics.health.HealthCheck;
import com.fasterxml.jackson.databind.node.NullNode;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import org.sdase.commons.server.opa.filter.model.OpaResponse;

public class PolicyExistsHealthCheck extends HealthCheck {

  public static final String DEFAULT_NAME = "OpenPolicyAgent";

  private final WebTarget client;

  public PolicyExistsHealthCheck(WebTarget client) {
    this.client = client;
  }

  @Override
  public Result check() {
    // send a get request to the policy path. The get will not provide any input.
    // Normally, the policy should response with a deny decision.
    // If there is an exception, the check will be unhealthy
    OpaResponse opaResponse = client.request().post(Entity.json(null), OpaResponse.class);

    if (opaResponse == null || opaResponse.getResult() instanceof NullNode) {
      return Result.unhealthy("The policy response seems not to be SDA guideline compliant");
    }

    if (opaResponse.isAllow()) {
      return Result.unhealthy("The policy should respond with a deny decision by default");
    }

    return Result.healthy();
  }
}
