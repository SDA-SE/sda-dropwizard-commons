package org.sdase.commons.server.healthcheck.servlet;

import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * Servlet that provides only the <b>internal</b> health check data of the application as JSON
 * response
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckServlet} when
 *     removing the module {@code sda-commons-server-healthcheck}. To prepare for the upcoming
 *     breaking change, update all references to {@link
 *     org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckServlet} and
 *     remove direct dependencies to {@code sda-commons-server-healthcheck}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class OnlyInternalHealthCheckServlet
    extends org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckServlet {
  public OnlyInternalHealthCheckServlet(HealthCheckRegistry healthCheckRegistry) {
    super(healthCheckRegistry);
  }
}
