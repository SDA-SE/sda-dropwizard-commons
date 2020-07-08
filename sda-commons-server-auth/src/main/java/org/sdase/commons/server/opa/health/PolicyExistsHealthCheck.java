package org.sdase.commons.server.opa.health;

import com.codahale.metrics.health.HealthCheck;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyExistsHealthCheck extends HealthCheck {

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyExistsHealthCheck.class);
  public static final String DEFAULT_NAME = "OpenPolicyAgent";

  private final WebTarget client;

  public PolicyExistsHealthCheck(WebTarget client) {
    this.client = client;
  }

  @Override
  public Result check() {

    try {

      // send a get request to the policy path. The get will not provide any input.
      // Normally, the policy should response with a deny decision.
      // If there is an exception, the check will be unhealthy
      OpaResponse opaResponse = client.request().post(Entity.json(null), OpaResponse.class);

      if (opaResponse == null
          || opaResponse.getResult() == null
          || opaResponse.getResult().isNull()) {
        LOGGER.warn("The policy response seems not to be SDA guideline compliant");
        return Result.unhealthy("The policy response seems not to be SDA guideline compliant");
      }

      if (opaResponse.isAllow()) {
        LOGGER.warn("The policy should respond with a deny decision by default");
        return Result.unhealthy("The policy should respond with a deny decision by default");
      }

      return Result.healthy();

    } catch (Exception e) {
      LOGGER.warn("Failed health check", e);
      return Result.unhealthy(e.getMessage());
    }
  }
}
