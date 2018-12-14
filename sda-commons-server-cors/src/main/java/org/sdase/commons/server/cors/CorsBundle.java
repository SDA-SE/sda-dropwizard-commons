package org.sdase.commons.server.cors;

import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import org.sdase.commons.shared.tracing.RequestTracing;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.List;

public class CorsBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private CorsConfigProvider<C> configProvider;

   private CorsBundle(CorsConfigProvider<C> configProvider) {
      this.configProvider = configProvider;
   }

   @Override
   public void run(C configuration, Environment environment) {
      CorsConfiguration config = configProvider.apply(configuration);

      FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

      // UrlPatterns where to apply the filter
      filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

      // Add URL mapping
      filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, String.join(",", config.getAllowedOrigins()));
      filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,PUT,DELETE,OPTIONS,HEAD,PATCH");

      List<String> allowedHeaders = Lists
            .newArrayList("Content-Type", "Authorization", "X-Requested-With", "Accept", ConsumerTracing.TOKEN_HEADER,
                  RequestTracing.TOKEN_HEADER);
      allowedHeaders.addAll(config.getAllowedHeaders());
      filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, String.join(",", allowedHeaders));

      List<String> exposedHeaders = Lists.newArrayList("Location");
      exposedHeaders.addAll(config.getExposedHeaders());
      filter.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM, String.join(",", exposedHeaders));

      filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");
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
      <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(CorsConfigProvider<C1> configProvider);
   }

   public interface FinalBuilder<C extends Configuration> {
      CorsBundle<C> build();
   }

   public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

      private CorsConfigProvider<C> configProvider = (c -> new CorsConfiguration());

      private Builder() {
      }

      private Builder(CorsConfigProvider<C> configProvider) {
         this.configProvider = configProvider;
      }

      @Override
      public CorsBundle<C> build() {
         return new CorsBundle<>(configProvider);
      }

      @Override
      public <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(CorsConfigProvider<C1> configProvider) {
         return new Builder<>(configProvider);
      }
   }

}
