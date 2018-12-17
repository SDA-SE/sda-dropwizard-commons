package org.sdase.commons.server.cors;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import org.sdase.commons.shared.tracing.RequestTracing;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class CorsBundle<C extends Configuration> implements ConfiguredBundle<C> {

   /**
    * The headers that clients are allowed to send when the {@code CorsBundle} is added to the application.
    */
   private static final Set<String> ALWAYS_ALLOWED_HEADERS = new LinkedHashSet<>(asList(
         HttpHeaders.CONTENT_TYPE,
         HttpHeaders.ACCEPT,
         HttpHeaders.AUTHORIZATION,
         "X-Requested-With",
         ConsumerTracing.TOKEN_HEADER,
         RequestTracing.TOKEN_HEADER
   ));

   /**
    * The headers that are allowed to be exposed from the response when the {@code CorsBundle} is added to the
    * application.
    */
   private static final Set<String> ALWAYS_EXPOSED_HEADERS = Collections.singleton(
         HttpHeaders.LOCATION
   );

   /**
    * The Http methods that are allowed when the {@code CorsBundle} is added to the application without further
    * configuration.
    */
   private static final Set<String> DEFAULT_HTTP_METHODS = new LinkedHashSet<>(asList(
         HttpMethod.HEAD,
         HttpMethod.GET,
         HttpMethod.POST,
         HttpMethod.PUT,
         HttpMethod.DELETE,
         "PATCH"
   ));

   private CorsConfigProvider<C> configProvider;

   private Set<String> allowedHttpMethods;

   private Set<String> allowedHeaders;

   private Set<String> exposedHeaders;

   private CorsBundle(
         CorsConfigProvider<C> configProvider,
         Set<String> allowedHttpMethods,
         Set<String> allowedHeaders,
         Set<String> exposedHeaders) {
      this.configProvider = configProvider;
      this.allowedHttpMethods = allowedHttpMethods;
      this.allowedHeaders = allowedHeaders;
      this.exposedHeaders = exposedHeaders;
   }

   @Override
   public void run(C configuration, Environment environment) {
      CorsConfiguration config = configProvider.apply(configuration);

      FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

      // UrlPatterns where to apply the filter
      filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

      filter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, String.join(",", config.getAllowedOrigins()));
      filter.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, String.join(",", allowedHttpMethods));
      filter.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, String.join(",", allowedHeaders));
      filter.setInitParameter(CrossOriginFilter.EXPOSED_HEADERS_PARAM, String.join(",", exposedHeaders));

      // TODO to be discussed with Timo Pagel: we do not use cookies or basic auth, so why do we allow a credentials param?
      filter.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, Boolean.TRUE.toString());

      // affects only pre flight requests, regular options mapping is still possible
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

      /**
       * <p>
       *    Defines the HTTP methods that are used in the application and should be allowed for cross origin clients.
       *    The given {@code httpMethods} overwrite the {@link #DEFAULT_HTTP_METHODS default methods}.
       * </p>
       * <ul>
       *     <li>{@value HttpMethod#OPTIONS}</li>
       *     <li>{@value HttpMethod#HEAD}</li>
       *     <li>{@value HttpMethod#GET}</li>
       *     <li>{@value HttpMethod#DELETE}</li>
       *     <li>{@value HttpMethod#POST}</li>
       *     <li>{@value HttpMethod#PUT}</li>
       *     <li>{@code "PATCH"}</li>
       * </ul>
       *
       * @param httpMethods all HTTP methods that will be allowed by the CORS filter.
       * @return the same builder instance
       */
      FinalBuilder<C> withAllowedMethods(String... httpMethods);

      /**
       * Defines additional HTTP headers that clients may send to the application. The headers defined in
       * {@link #ALWAYS_ALLOWED_HEADERS} are always allowed to be send and must not be added here.
       *
       * @param additionalAllowedHeaders additional HTTP headers that clients may send to the application.
       * @return the same builder instance
       */
      FinalBuilder<C> withAdditionalAllowedHeaders(String... additionalAllowedHeaders);

      /**
       * Defines additional HTTP headers that can be exposed to clients. The headers defined in
       * {@link #ALWAYS_EXPOSED_HEADERS} can always be exposed and must not be added here.
       *
       * @param additionalExposedHeaders additional HTTP headers that can be exposed.
       * @return the same builder instance
       */
      FinalBuilder<C> withAdditionalExposedHeaders(String... additionalExposedHeaders);

      CorsBundle<C> build();
   }

   public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

      private Set<String> allowedHttpMethods = new LinkedHashSet<>(DEFAULT_HTTP_METHODS);
      private Set<String> allowedHeaders = new LinkedHashSet<>(ALWAYS_ALLOWED_HEADERS);
      private Set<String> exposedHeaders = new LinkedHashSet<>(ALWAYS_EXPOSED_HEADERS);

      private CorsConfigProvider<C> configProvider = (c -> new CorsConfiguration());

      private Builder() {
      }

      private Builder(CorsConfigProvider<C> configProvider) {
         this.configProvider = configProvider;
      }

      @Override
      public <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(CorsConfigProvider<C1> configProvider) {
         return new Builder<>(configProvider);
      }

      @Override
      public FinalBuilder<C> withAllowedMethods(String... httpMethods) {
         this.allowedHttpMethods = Stream.of(httpMethods)
               .filter(Objects::nonNull)
               .collect(Collectors.toCollection(LinkedHashSet::new));
         return this;
      }

      @Override
      public FinalBuilder<C> withAdditionalAllowedHeaders(String... additionalAllowedHeaders) {
         Stream.of(additionalAllowedHeaders).filter(Objects::nonNull).forEach(this.allowedHeaders::add);
         return this;
      }

      @Override
      public FinalBuilder<C> withAdditionalExposedHeaders(String... additionalExposedHeaders) {
         Stream.of(additionalExposedHeaders).filter(Objects::nonNull).forEach(this.exposedHeaders::add);
         return this;
      }

      @Override
      public CorsBundle<C> build() {
         return new CorsBundle<>(configProvider, allowedHttpMethods, allowedHeaders, exposedHeaders);
      }
   }

}
