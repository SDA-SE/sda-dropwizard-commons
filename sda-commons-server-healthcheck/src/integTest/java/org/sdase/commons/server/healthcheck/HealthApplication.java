package org.sdase.commons.server.healthcheck;

import com.codahale.metrics.health.HealthCheck;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.healthcheck.bundle.InternalHealthCheckEndpointBundle;
import org.sdase.commons.server.healthcheck.bundle.ExternalHealthCheck;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

public class HealthApplication extends Application<Configuration> {

   static final String DUMMY_INTERNAL = "dummyInternal";
   static final String DUMMY_EXTERNAL = "dummyExternal";
   private boolean isHealthy = true;


   @Override
   public void initialize(final Bootstrap<Configuration> bootstrap) {
      bootstrap.addBundle(InternalHealthCheckEndpointBundle.builder().build());
   }

   @Override
   public void run(final Configuration config, final Environment environment)  {
      environment.healthChecks().register(DUMMY_INTERNAL,
            new HealthCheck() {
         @Override
         protected Result check() {
            if (isHealthy) {
               return Result.healthy(DUMMY_INTERNAL);
            } else {
               return Result.unhealthy(DUMMY_INTERNAL);
            }
         }
      });
      environment.healthChecks().register(DUMMY_EXTERNAL, new ExternalHealthCheckDummy());
   }

   protected class ExternalHealthCheckDummy extends HealthCheck implements ExternalHealthCheck {
      @Override
      protected Result check() {
         return Result.healthy(DUMMY_EXTERNAL);
      }
   }

   /**
    * Method to influence the behavior within the test
    * @param healthy status of internal health check returns
    */
    void setHealthy(boolean healthy) {
      isHealthy = healthy;
   }
}
