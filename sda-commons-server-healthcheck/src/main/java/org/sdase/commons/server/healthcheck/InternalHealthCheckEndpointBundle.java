package org.sdase.commons.server.healthcheck;

/**
 * This bundle registers a new servlet to provide all registered health checks that are not
 * {@linkplain ExternalHealthCheck external}. The health checks are provided at {@code
 * /healthcheck/internal} at the admin port.
 *
 * @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.InternalHealthCheckEndpointBundle} when removing
 *     the module {@code sda-commons-server-healthcheck}. To prepare for the upcoming breaking
 *     change, update all references to {@link
 *     org.sdase.commons.server.dropwizard.bundles.InternalHealthCheckEndpointBundle} and remove
 *     direct dependencies to {@code sda-commons-server-healthcheck}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class InternalHealthCheckEndpointBundle
    extends org.sdase.commons.server.dropwizard.bundles.InternalHealthCheckEndpointBundle {

  private InternalHealthCheckEndpointBundle() {
    super();
  }

  public static Builder builder() {
    return new Builder();
  }

  public interface InternalHealthCheckEndpointBuilder
      extends org.sdase.commons.server.dropwizard.bundles.InternalHealthCheckEndpointBundle
          .InternalHealthCheckEndpointBuilder {
    InternalHealthCheckEndpointBundle build();
  }

  public static class Builder
      extends org.sdase.commons.server.dropwizard.bundles.InternalHealthCheckEndpointBundle.Builder
      implements InternalHealthCheckEndpointBuilder {

    private Builder() {
      super();
    }

    @Override
    public InternalHealthCheckEndpointBundle build() {
      return new InternalHealthCheckEndpointBundle();
    }
  }
}
