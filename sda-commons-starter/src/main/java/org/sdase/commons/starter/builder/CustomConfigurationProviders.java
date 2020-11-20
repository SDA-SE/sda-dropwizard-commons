package org.sdase.commons.starter.builder;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.cors.CorsConfigProvider;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.starter.builder.OpenApiCustomizer.OpenApiInitialBuilder;

/**
 * Container for builder interfaces required for custom configuration classes. They are all in one
 * place in the order how they are called for easier extension with more supported bundles.
 */
public interface CustomConfigurationProviders {

  interface AuthConfigProviderBuilder<C extends Configuration> {

    /**
     * Disable authentication entirely.
     *
     * @return the builder instance
     */
    CorsConfigProviderBuilder<C> withoutAuthentication();

    /**
     * Enables authentication (i.e. token validation) for annotated endpoints and requires tokens
     *
     * @param authConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.auth.config.AuthConfig}, e.g. {@code MyAppConfig::getAuth}
     * @return the builder instance
     */
    CorsConfigProviderBuilder<C> withAuthConfigProvider(AuthConfigProvider<C> authConfigProvider);

    /**
     * Enables authentication (i.e. token validation) for all endpoints and use the {@link
     * org.sdase.commons.server.opa.OpaBundle} to authorize the requests. Requests without
     * Authorization header will <i>not</i> be rejected but need to be handled in the Authorization
     * policy.
     *
     * @param authConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.auth.config.AuthConfig}, e.g. {@code MyAppConfig::getAuth}
     * @param opaConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.opa.config.OpaConfig}, e.g. {@code MyAppConfig::getOpa}
     * @return the builder instance
     */
    CorsConfigProviderBuilder<C> withOpaAuthorization(
        AuthConfigProvider<C> authConfigProvider, OpaConfigProvider<C> opaConfigProvider);
  }

  interface CorsConfigProviderBuilder<C extends Configuration> {

    /**
     * Disable CORS support. Browsers are not allowed to access the application from a foreign
     * domain.
     *
     * @return the builder instance
     */
    OpenApiInitialBuilder<C> withoutCorsSupport();

    /**
     * @param corsConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.cors.CorsConfiguration}, e.g. {@code MyAppConfig::getCors}
     * @return the builder instance
     */
    OpenApiInitialBuilder<C> withCorsConfigProvider(CorsConfigProvider<C> corsConfigProvider);
  }
}
