package org.sdase.commons.server.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Configuration;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.healthcheck.helper.ExternalServiceHealthCheck;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;

public class HealthCheckIT {

   private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckIT.class);

   @ClassRule
   public static final DropwizardAppRule<Configuration> RULE =
         DropwizardRuleHelper.dropwizardTestAppFrom(HealthApplication.class)
         .withConfigFrom(Configuration::new)
         .withRandomPorts()
         .build();

   @Test
   public void testAllHealthChecks() {
      setApplicationHealth(true);
      Response response = healthCheckGet("/healthcheck");
      String metrics = response.readEntity(String.class);
      assertThat(metrics).contains(HealthApplication.DUMMY_EXTERNAL);
      assertThat(metrics).contains(HealthApplication.DUMMY_INTERNAL);
   }

   @Test
   public void testHealthChecksInternal() {
      setApplicationHealth(true);
      Response response = healthCheckGet("/healthcheck/internal");
      assertThat(response.getStatus()).isEqualTo(SC_OK);
      String metrics = response.readEntity(String.class);
      LOGGER.info("Prometheus metrics: {}", metrics);

      assertThat(metrics).doesNotContain(HealthApplication.DUMMY_EXTERNAL);
      assertThat(metrics).contains(HealthApplication.DUMMY_INTERNAL);
   }

   @Test
   public void shouldBeUnhealthy() {
      setApplicationHealth(false);
      Response response = healthCheckGet("/healthcheck");
      assertThat(response.getStatus()).isNotEqualTo(SC_OK);

      String metrics = response.readEntity(String.class);
      assertThat(metrics).contains(HealthApplication.DUMMY_EXTERNAL);
      assertThat(metrics).contains(HealthApplication.DUMMY_INTERNAL);
   }

   @Test
   public void shouldBeUnhealthyInternal() {
      setApplicationHealth(false);
      Response response = healthCheckGet("/healthcheck/internal");
      assertThat(response.getStatus()).isNotEqualTo(SC_OK);

      String metrics = response.readEntity(String.class);
      assertThat(metrics).doesNotContain(HealthApplication.DUMMY_EXTERNAL);
      assertThat(metrics).contains(HealthApplication.DUMMY_INTERNAL);
   }

   @Test
   public void testRegressionRunHealthCheckSuccessfulTwice() throws Exception {
      ExternalServiceHealthCheck healthCheck = new ExternalServiceHealthCheck(
            String.format("http://localhost:%d/metrics", RULE.getAdminPort()), 1000);

      HealthCheck.Result firstResult = healthCheck.check();
      HealthCheck.Result secondResult = healthCheck.check();

      assertThat(firstResult.isHealthy()).isTrue();
      assertThat(secondResult.isHealthy()).isTrue();
   }

   private void setApplicationHealth(boolean isHealthy) {
      HealthApplication app = RULE.getApplication();
      app.setHealthy(isHealthy);
   }

   private Response healthCheckGet(String s) {
      return RULE
            .client()
            .target(String.format("http://localhost:%d", RULE.getAdminPort()) + s)
            .request()
            .get();
   }

}

