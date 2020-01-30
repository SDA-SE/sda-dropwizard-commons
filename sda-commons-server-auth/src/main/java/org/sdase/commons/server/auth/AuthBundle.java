package org.sdase.commons.server.auth;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import javax.ws.rs.client.Client;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.auth.config.KeyLocation;
import org.sdase.commons.server.auth.error.ForbiddenExceptionMapper;
import org.sdase.commons.server.auth.error.JwtAuthExceptionMapper;
import org.sdase.commons.server.auth.filter.JwtAuthFilter;
import org.sdase.commons.server.auth.key.JwksKeySource;
import org.sdase.commons.server.auth.key.KeySource;
import org.sdase.commons.server.auth.key.OpenIdProviderDiscoveryKeySource;
import org.sdase.commons.server.auth.key.PemKeySource;
import org.sdase.commons.server.auth.key.RsaPublicKeyLoader;
import org.sdase.commons.server.auth.service.AuthRSA256Service;
import org.sdase.commons.server.auth.service.AuthService;
import org.sdase.commons.server.auth.service.JwtAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AuthBundle.class);

  private AuthConfigProvider<T> configProvider;
  private boolean useAnnotatedAuthorization;

  public static ProviderBuilder builder() {
    return new Builder();
  }

  private AuthBundle(AuthConfigProvider<T> configProvider, boolean useAnnotatedAuthorization) {
    this.configProvider = configProvider;
    this.useAnnotatedAuthorization = useAnnotatedAuthorization;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // no initialization needed
  }

  @Override
  public void run(T configuration, Environment environment) {

    AuthConfig config = configProvider.apply(configuration);

    if (config.isDisableAuth()) {
      LOG.warn("Authentication is disabled. This setting should NEVER be used in production.");
    }

    Client client = new JerseyClientBuilder(environment).build("keyLoader");
    RsaPublicKeyLoader keyLoader = new RsaPublicKeyLoader();
    config.getKeys().stream()
        .map(k -> this.createKeySources(k, client))
        .forEach(keyLoader::addKeySource);

    AuthService authRSA256Service = new AuthRSA256Service(keyLoader, config.getLeeway());
    JwtAuthenticator authenticator =
        new JwtAuthenticator(authRSA256Service, config.isDisableAuth());

    JwtAuthFilter<JwtPrincipal> authFilter =
        new JwtAuthFilter.Builder<JwtPrincipal>()
            .setAcceptAnonymous(!useAnnotatedAuthorization)
            .setAuthenticator(authenticator)
            .buildAuthFilter();

    if (useAnnotatedAuthorization) {
      // Use the AuthDynamicFeature to only affect endpoints that are
      // annotated
      environment.jersey().register(new AuthDynamicFeature(authFilter));
    } else {
      // Apply the filter for all calls
      environment.jersey().register(authFilter);
    }

    environment.jersey().register(JwtAuthExceptionMapper.class);
    environment.jersey().register(ForbiddenExceptionMapper.class);
  }

  private KeySource createKeySources(KeyLocation keyLocation, Client client) {
    switch (keyLocation.getType()) {
      case PEM:
        return new PemKeySource(keyLocation.getPemKeyId(), keyLocation.getLocation());
      case OPEN_ID_DISCOVERY:
        return new OpenIdProviderDiscoveryKeySource(
            keyLocation.getLocation().toASCIIString(), client);
      case JWKS:
        return new JwksKeySource(keyLocation.getLocation().toASCIIString(), client);
      default:
        throw new IllegalArgumentException(
            "KeyLocation has no valid type: " + keyLocation.getType());
    }
  }

  //
  // Builder
  //

  public interface ProviderBuilder {
    <C extends Configuration> AuthorizationBuilder<C> withAuthConfigProvider(
        AuthConfigProvider<C> authConfigProvider);
  }

  public interface AuthorizationBuilder<C extends Configuration> {
    /**
     * Configures the bundle to require valid tokens for all endpoints that are annotated with
     * {@code @PermitAll}.
     *
     * @return the builder
     */
    AuthBuilder<C> withAnnotatedAuthorization();

    /**
     * Configures the bundle to validate tokens but also permit requests without Authorization
     * header. Authorization decisions need be made separately e.g. by the {@link
     * org.sdase.commons.server.opa.OpaBundle}.
     *
     * @return the builder
     */
    AuthBuilder<C> withExternalAuthorization();
  }

  public interface AuthBuilder<C extends Configuration> {
    AuthBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      implements ProviderBuilder, AuthorizationBuilder<C>, AuthBuilder<C> {

    private AuthConfigProvider<C> authConfigProvider;
    private boolean useAnnotatedAuthorization = true;

    private Builder() {}

    private Builder(AuthConfigProvider<C> authConfigProvider) {
      this.authConfigProvider = authConfigProvider;
    }

    @Override
    public <T extends Configuration> AuthorizationBuilder<T> withAuthConfigProvider(
        AuthConfigProvider<T> authConfigProvider) {
      return new Builder<>(authConfigProvider);
    }

    @Override
    public AuthBuilder<C> withAnnotatedAuthorization() {
      this.useAnnotatedAuthorization = true;
      return this;
    }

    @Override
    public AuthBuilder<C> withExternalAuthorization() {
      this.useAnnotatedAuthorization = false;
      return this;
    }

    @Override
    public AuthBundle<C> build() {
      return new AuthBundle<>(authConfigProvider, useAnnotatedAuthorization);
    }
  }
}
