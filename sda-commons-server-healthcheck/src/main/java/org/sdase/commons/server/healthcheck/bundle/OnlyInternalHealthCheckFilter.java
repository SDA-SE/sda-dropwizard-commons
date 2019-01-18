package org.sdase.commons.server.healthcheck.bundle;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckFilter;
import org.sdase.commons.server.healthcheck.ExternalHealthCheck;

/**
 * filter that removes all health checks that implements the marker interface
 * {@link ExternalHealthCheck}
 */
public class OnlyInternalHealthCheckFilter implements HealthCheckFilter {

   @Override
   public boolean matches(String name, HealthCheck healthCheckFilter) {
      return !(healthCheckFilter instanceof ExternalHealthCheck);
   }

}
