package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.healthcheck.servlet.OnlyInternalHealthCheckServlet;

/**
 * This bundle registers a new servlet to provide all registered health checks that are not
 * {@linkplain org.sdase.commons.server.dropwizard.healthcheck.ExternalHealthCheck external}. The
 * health checks are provided at {@code /healthcheck/internal} at the admin port.
 */
public class InternalHealthCheckEndpointBundle implements ConfiguredBundle<Configuration> {

  // TODO next-major consider this constructor as private and remove when deprecated subclass is
  // removed
  protected InternalHealthCheckEndpointBundle() {
    // deny public access
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // Register a new endpoints that provides only the internal health checks
    // The default healthcheck endpoint '/healthcheck' provides both, internal and external
    // health checks
    environment
        .admin()
        .addServlet(
            "Internal Health Check", new OnlyInternalHealthCheckServlet(environment.healthChecks()))
        .addMapping("/healthcheck/internal");
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // Nothing here
  }

  public static Builder builder() {
    return new Builder();
  }

  public interface InternalHealthCheckEndpointBuilder {
    InternalHealthCheckEndpointBundle build();
  }

  public static class Builder implements InternalHealthCheckEndpointBuilder {

    // TODO next-major consider this constructor as private and remove when deprecated subclass is
    // removed
    protected Builder() {
      // deny public access
    }

    @Override
    public InternalHealthCheckEndpointBundle build() {
      return new InternalHealthCheckEndpointBundle();
    }
  }
}
