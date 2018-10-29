package com.sdase.commons.server.auth;

import com.sdase.commons.server.auth.config.AuthConfig;
import com.sdase.commons.server.auth.config.AuthConfigProvider;
import com.sdase.commons.server.auth.config.KeyLocation;
import com.sdase.commons.server.auth.filter.JwtAuthFilter;
import com.sdase.commons.server.auth.key.JwksKeySource;
import com.sdase.commons.server.auth.key.KeySource;
import com.sdase.commons.server.auth.key.OpenIdProviderDiscoveryKeySource;
import com.sdase.commons.server.auth.key.PemKeySource;
import com.sdase.commons.server.auth.key.RsaPublicKeyLoader;
import com.sdase.commons.server.auth.service.AuthRSA256Service;
import com.sdase.commons.server.auth.service.AuthService;
import com.sdase.commons.server.auth.service.JwtAuthenticator;
import com.sdase.commons.server.auth.service.JwtAuthorizer;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;

public class AuthBundle<T extends Configuration> implements ConfiguredBundle<T> {

   private static final Logger LOG = LoggerFactory.getLogger(AuthBundle.class);

   private AuthConfigProvider<T> configProvider;

   public static ProviderBuilder builder() {
      return new Builder();
   }

   private AuthBundle(AuthConfigProvider<T> configProvider) {
      this.configProvider = configProvider;
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
      config.getKeys().stream().map(k -> this.createKeySources(k, client)).forEach(keyLoader::addKeySource);

      AuthService authRSA256Service = new AuthRSA256Service(keyLoader, config.getLeeway());
      JwtAuthenticator authenticator = new JwtAuthenticator(authRSA256Service, config.isDisableAuth());
      JwtAuthorizer authorizer = new JwtAuthorizer(config.isDisableAuth());

      JwtAuthFilter<JwtPrincipal> authFilter = new JwtAuthFilter.Builder<JwtPrincipal>()
            .setAuthenticator(authenticator)
            .setAuthorizer(authorizer)
            .buildAuthFilter();
      environment.jersey().register(new AuthDynamicFeature(authFilter));

      environment.jersey().register(RolesAllowedDynamicFeature.class);

   }

   private KeySource createKeySources(KeyLocation keyLocation, Client client) {
      switch (keyLocation.getType()) {
         case PEM:
            return new PemKeySource(keyLocation.getPemKeyId(), keyLocation.getLocation());
         case OPEN_ID_DISCOVERY:
            return new OpenIdProviderDiscoveryKeySource(keyLocation.getLocation().toASCIIString(), client);
         case JWKS:
            return new JwksKeySource(keyLocation.getLocation().toASCIIString(), client);
         default:
            throw new IllegalArgumentException("KeyLocation has no valid type: " + keyLocation.getType());
      }
   }

   //
   // Builder
   //

   public interface ProviderBuilder {
      <C extends Configuration> AuthBuilder<C> withAuthConfigProvider(AuthConfigProvider<C> authConfigProvider);
   }

   public interface AuthBuilder<C extends Configuration> {
      AuthBundle<C> build();
   }

   public static class Builder<C extends Configuration> implements ProviderBuilder, AuthBuilder<C> {

      private AuthConfigProvider<C> authConfigProvider;

      private Builder() {
      }

      private Builder(AuthConfigProvider<C> authConfigProvider) {
         this.authConfigProvider = authConfigProvider;
      }

      @Override
      public <T extends Configuration> AuthBuilder<T> withAuthConfigProvider(AuthConfigProvider<T> authConfigProvider) {
         return new Builder<>(authConfigProvider);
      }

      @Override
      public AuthBundle<C> build() {
         return new AuthBundle<>(authConfigProvider);
      }
   }
}
