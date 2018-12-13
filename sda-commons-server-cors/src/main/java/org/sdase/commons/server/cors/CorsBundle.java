package org.sdase.commons.server.cors;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

public class CorsBundle<T extends Configuration> implements ConfiguredBundle<T> {

   private CorsConfigProvider<T> configProvider;

   private CorsBundle(CorsConfigProvider<T> configProvider) {
      this.configProvider = configProvider;
   }

   @Override
   public void run(T configuration, Environment environment) {
      CorsConfiguration config = configProvider.apply(configuration);

      FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

      // Add URL mapping
      filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "*");
      filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,DELETE,OPTIONS");
      filter
            .setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM,
                  "http://localhost:*," + String.join(",", config.getAllowedOrigins()));
      filter
            .setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
                  "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin");
      filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
      filter.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM, "Location");
      filter.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, Boolean.FALSE.toString());
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // do nothing here
   }

   public static InitialBuilder builder() {
      return new Builder();
   }

   public interface InitialBuilder {
      <C extends Configuration> FinalBuilder withCorsConfigProvider(CorsConfigProvider<C> configProvider);
   }

   public interface FinalBuilder<C extends Configuration> {
      CorsBundle<C> build();
   }

   public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

      private CorsConfigProvider<C> configProvider;

      private Builder() {
      }

      private Builder(CorsConfigProvider<C> configProvider) {
         this.configProvider = configProvider;
      }

      @Override
      public <T extends Configuration> FinalBuilder<T> withCorsConfigProvider(CorsConfigProvider<T> configProvider) {
         return new Builder<>(configProvider);
      }

      @Override
      public CorsBundle<C> build() {
         return new CorsBundle<>(configProvider);
      }
   }

}
