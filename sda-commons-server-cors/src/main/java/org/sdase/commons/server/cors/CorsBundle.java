package org.sdase.commons.server.cors;

import io.dropwizard.core.Configuration;
import jakarta.ws.rs.HttpMethod;
import java.util.*;

/* @deprecated This class will be replaced by {@link
 *     org.sdase.commons.server.dropwizard.bundles.CorsBundle.CorsConfigProvider} when removing the
 *     module {@code sda-commons-server-cors}. To prepare for the upcoming breaking change, update
 *     all references to {@link
 *     org.sdase.commons.server.dropwizard.bundles.CorsBundle.CorsConfigProvider} and remove direct
 *     dependencies to {@code sda-commons-server-cors}.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("java:S2176") // intentionally the same name until removed
public class CorsBundle<C extends Configuration>
    extends org.sdase.commons.server.dropwizard.bundles.CorsBundle<C> {

  private CorsBundle(org.sdase.commons.server.dropwizard.bundles.CorsBundle<C> corsBundle) {
    super(corsBundle);
  }

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  public interface InitialBuilder
      extends org.sdase.commons.server.dropwizard.bundles.CorsBundle.InitialBuilder {
    <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(
        org.sdase.commons.server.cors.CorsConfigProvider<C1> configProvider);
  }

  public interface FinalBuilder<C extends Configuration>
      extends org.sdase.commons.server.dropwizard.bundles.CorsBundle.FinalBuilder<C> {

    /**
     * Defines the HTTP methods that are used in the application and should be allowed for
     * cross-origin clients. The given {@code httpMethods} overwrite the default methods.
     *
     * <ul>
     *   <li>{@value HttpMethod#OPTIONS}
     *   <li>{@value HttpMethod#HEAD}
     *   <li>{@value HttpMethod#GET}
     *   <li>{@value HttpMethod#DELETE}
     *   <li>{@value HttpMethod#POST}
     *   <li>{@value HttpMethod#PUT}
     *   <li>{@code "PATCH"}
     * </ul>
     *
     * @param httpMethods all HTTP methods that will be allowed by the CORS filter.
     * @return the same builder instance
     */
    FinalBuilder<C> withAllowedMethods(String... httpMethods);

    /**
     * Defines additional HTTP headers that clients may send to the application. The headers {@code
     * Content-type}, {@code Accept}, {@code Authorization}, {@code X-Requested-With}, {@code
     * Consumer-token} and {@code Trace-Token} are always allowed to be sent and must not be added
     * here.
     *
     * @param additionalAllowedHeaders additional HTTP headers that clients may send to the
     *     application.
     * @return the same builder instance
     */
    FinalBuilder<C> withAdditionalAllowedHeaders(String... additionalAllowedHeaders);

    /**
     * Defines additional HTTP headers that can be exposed to clients. The headers {@code
     * Content-type}, {@code Accept}, {@code Authorization}, {@code X-Requested-With}, {@code
     * Consumer-token} and {@code Trace-Token} can always be exposed and must not be added here.
     *
     * @param additionalExposedHeaders additional HTTP headers that can be exposed.
     * @return the same builder instance
     */
    FinalBuilder<C> withAdditionalExposedHeaders(String... additionalExposedHeaders);

    CorsBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      extends org.sdase.commons.server.dropwizard.bundles.CorsBundle.Builder<C>
      implements InitialBuilder, FinalBuilder<C> {

    private Builder() {
      super();
    }

    private Builder(CorsConfigProvider<C> configProvider) {
      super(configProvider);
    }

    @Override
    public <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(
        org.sdase.commons.server.cors.CorsConfigProvider<C1> configProvider) {
      return new Builder<>(configProvider);
    }

    @Override
    public FinalBuilder<C> withAllowedMethods(String... httpMethods) {
      super.withAllowedMethods(httpMethods);
      return this;
    }

    @Override
    public FinalBuilder<C> withAdditionalAllowedHeaders(String... additionalAllowedHeaders) {
      super.withAdditionalAllowedHeaders(additionalAllowedHeaders);
      return this;
    }

    @Override
    public FinalBuilder<C> withAdditionalExposedHeaders(String... additionalExposedHeaders) {
      super.withAdditionalExposedHeaders(additionalExposedHeaders);
      return this;
    }

    @Override
    public CorsBundle<C> build() {
      return new CorsBundle<>(super.build());
    }
  }
}
