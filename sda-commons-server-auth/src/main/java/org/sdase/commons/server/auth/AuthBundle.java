package org.sdase.commons.server.auth;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;
import javax.ws.rs.client.Client;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.auth.config.KeyLocation;
import org.sdase.commons.server.auth.error.ForbiddenExceptionMapper;
import org.sdase.commons.server.auth.error.JwtAuthExceptionMapper;
import org.sdase.commons.server.auth.filter.JwtAuthFilter;
import org.sdase.commons.server.auth.key.JwksKeySource;
import org.sdase.commons.server.auth.key.KeyLoaderScheduler;
import org.sdase.commons.server.auth.key.KeySource;
import org.sdase.commons.server.auth.key.OpenIdProviderDiscoveryKeySource;
import org.sdase.commons.server.auth.key.PemKeySource;
import org.sdase.commons.server.auth.key.PublicKeyLoader;
import org.sdase.commons.server.auth.service.AuthService;
import org.sdase.commons.server.auth.service.JwtAuthenticator;
import org.sdase.commons.server.auth.service.TokenAuthorizer;
import org.sdase.commons.server.opentelemetry.client.TracedHttpClientInitialBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AuthBundle.class);
  private static final String INSTRUMENTATION_NAME = "sda-commons.auth-bundle";

  private AuthConfigProvider<T> configProvider;
  private boolean useAnnotatedAuthorization;
  private final OpenTelemetry openTelemetry;

  public static ProviderBuilder builder() {
    return new Builder<>();
  }

  private AuthBundle(
      AuthConfigProvider<T> configProvider,
      boolean useAnnotatedAuthorization,
      OpenTelemetry openTelemetry) {
    this.configProvider = configProvider;
    this.useAnnotatedAuthorization = useAnnotatedAuthorization;
    this.openTelemetry = openTelemetry;
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

    // Initialize a telemetry instance if not set.
    OpenTelemetry currentTelemetryInstance =
        this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry;

    Client client = createKeyLoaderClient(environment, config, currentTelemetryInstance);
    PublicKeyLoader keyLoader = new PublicKeyLoader();
    config.getKeys().stream()
        .map(k -> this.createKeySources(k, client))
        .forEach(keyLoader::addKeySource);

    ScheduledExecutorService executorService =
        environment.lifecycle().scheduledExecutorService("reloadKeysExecutorService").build();
    KeyLoaderScheduler.create(keyLoader, executorService).start();

    TokenAuthorizer authService = new AuthService(keyLoader, config.getLeeway());
    JwtAuthenticator authenticator = new JwtAuthenticator(authService, config.isDisableAuth());

    JwtAuthFilter<JwtPrincipal> authFilter =
        new JwtAuthFilter.Builder<JwtPrincipal>()
            .withTracer(currentTelemetryInstance.getTracer(INSTRUMENTATION_NAME))
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

  private Client createKeyLoaderClient(
      Environment environment, AuthConfig config, OpenTelemetry openTelemetry) {
    JerseyClientBuilder jerseyClientBuilder = new JerseyClientBuilder(environment);
    // should be set as soon as creating the builder
    jerseyClientBuilder.setApacheHttpClientBuilder(
        new TracedHttpClientInitialBuilder(environment).usingTelemetryInstance(openTelemetry));

    // a specific proxy configuration always overrides the system proxy
    if (config.getKeyLoaderClient() == null
        || config.getKeyLoaderClient().getProxyConfiguration() == null) {
      // register a route planner that uses the default proxy variables (e.g. http.proxyHost)
      jerseyClientBuilder.using(new SystemDefaultRoutePlanner(ProxySelector.getDefault()));
    }

    if (config.getKeyLoaderClient() != null) {
      jerseyClientBuilder.using(config.getKeyLoaderClient());
    }

    return jerseyClientBuilder.build("keyLoader");
  }

  private KeySource createKeySources(KeyLocation keyLocation, Client client) {
    switch (keyLocation.getType()) {
      case PEM:
        return new PemKeySource(
            keyLocation.getPemKeyId(),
            keyLocation.getPemSignAlg(),
            keyLocation.getLocation(),
            keyLocation.getRequiredIssuer());
      case OPEN_ID_DISCOVERY:
        validateKeyLocation(keyLocation.getLocation(), keyLocation.getRequiredIssuer());
        return new OpenIdProviderDiscoveryKeySource(
            keyLocation.getLocation().toASCIIString(), client, keyLocation.getRequiredIssuer());
      case JWKS:
        validateKeyLocation(keyLocation.getLocation(), keyLocation.getRequiredIssuer());
        return new JwksKeySource(
            keyLocation.getLocation().toASCIIString(), client, keyLocation.getRequiredIssuer());
      default:
        throw new IllegalArgumentException(
            "KeyLocation has no valid type: " + keyLocation.getType());
    }
  }

  /**
   * Validate, that if a required issuer is set as URI the host name of the key location source and
   * the requiredIssuer must be the same.
   *
   * @param location The source {@link URI} of the key(s) as discovery or jwks endpoint.
   * @param requiredIssuer The required issuer as {@link String} for the correlated key source.
   */
  private void validateKeyLocation(URI location, String requiredIssuer) {
    if (StringUtils.isNotBlank(requiredIssuer) && StringUtils.contains(requiredIssuer, ':')) {
      try {
        URI issuerUri = new URI(requiredIssuer);
        if (!StringUtils.equalsIgnoreCase(location.getHost(), issuerUri.getHost())) {
          LOG.warn(
              "The required issuer host name <{}> for the key <{}> does not match to the key"
                  + " source uri host name <{}>.",
              issuerUri.getHost(),
              location,
              location.getHost());
        }
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException(
            "The requiredIssuer <" + requiredIssuer + "> is no valid stringOrURI", e);
      }
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

    AuthBuilder<C> withTelemetryInstance(OpenTelemetry openTelemetry);

    AuthBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      implements ProviderBuilder, AuthorizationBuilder<C>, AuthBuilder<C> {

    private AuthConfigProvider<C> authConfigProvider;
    private boolean useAnnotatedAuthorization = true;
    private OpenTelemetry openTelemetry;

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
    public AuthBuilder<C> withTelemetryInstance(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry;
      return this;
    }

    @Override
    public AuthBundle<C> build() {
      return new AuthBundle<>(authConfigProvider, useAnnotatedAuthorization, openTelemetry);
    }
  }
}
