package org.sdase.commons.server.cors;

import static java.util.Arrays.asList;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jetty.server.handler.CrossOriginHandler;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import org.sdase.commons.shared.tracing.TraceTokenContext;

public class CorsBundle<C extends Configuration> implements ConfiguredBundle<C> {

  /**
   * The headers that clients are allowed to send when the {@code CorsBundle} is added to the
   * application.
   */
  private static final Set<String> ALWAYS_ALLOWED_HEADERS =
      new LinkedHashSet<>(
          asList(
              HttpHeaders.CONTENT_TYPE,
              HttpHeaders.ACCEPT,
              HttpHeaders.AUTHORIZATION,
              "X-Requested-With",
              ConsumerTracing.TOKEN_HEADER,
              TraceTokenContext.TRACE_TOKEN_HTTP_HEADER_NAME));

  /**
   * The headers that are allowed to be exposed from the response when the {@code CorsBundle} is
   * added to the application.
   */
  private static final Set<String> ALWAYS_EXPOSED_HEADERS =
      Collections.singleton(HttpHeaders.LOCATION);

  /**
   * The Http methods that are allowed when the {@code CorsBundle} is added to the application
   * without further configuration.
   */
  private static final Set<String> DEFAULT_HTTP_METHODS =
      new LinkedHashSet<>(
          asList(
              HttpMethod.HEAD,
              HttpMethod.GET,
              HttpMethod.POST,
              HttpMethod.PUT,
              HttpMethod.DELETE,
              "PATCH"));

  private final CorsConfigProvider<C> configProvider;

  private final Set<String> allowedHttpMethods;

  private final Set<String> allowedHeaders;

  private final Set<String> exposedHeaders;

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

    CrossOriginHandler cors = new CrossOriginHandler();

    cors.setAllowedOriginPatterns(createCorsOriginPattern(config));
    cors.setAllowedMethods(allowedHttpMethods);
    cors.setAllowedHeaders(allowedHeaders);
    cors.setExposedHeaders(exposedHeaders);
    cors.setAllowCredentials(true);
    cors.setDeliverPreflightRequests(false);

    environment.getApplicationContext().insertHandler(cors);
  }

  private Set<String> createCorsOriginPattern(CorsConfiguration config) {
    return config.getAllowedOrigins().stream()
        .map(this::wildcardToRegex)
        .collect(Collectors.toSet());
  }

  private String wildcardToRegex(String origin) {
    if ("*".equals(origin)) {
      return ".*"; // match any origin
    }
    // Split on '*' and quote each literal segment, then join with ".*"
    String[] parts = origin.split("\\*", -1); // keep empty segments
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      sb.append(Pattern.quote(parts[i]));
      if (i < parts.length - 1) {
        sb.append(".*");
      }
    }
    // Optional: anchor to full string so the regex matches the entire Origin header.
    // Jetty examples typically don't add ^/$, but it's safer to control exact-matching:
    return "^" + sb.toString() + "$";
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // do nothing here
  }

  public static InitialBuilder builder() {
    return new Builder<>();
  }

  public interface InitialBuilder {
    <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(
        CorsConfigProvider<C1> configProvider);
  }

  public interface FinalBuilder<C extends Configuration> {

    /**
     * Defines the HTTP methods that are used in the application and should be allowed for cross
     * origin clients. The given {@code httpMethods} overwrite the {@link #DEFAULT_HTTP_METHODS
     * default methods}.
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
     * Defines additional HTTP headers that clients may send to the application. The headers defined
     * in {@link #ALWAYS_ALLOWED_HEADERS} are always allowed to be send and must not be added here.
     *
     * @param additionalAllowedHeaders additional HTTP headers that clients may send to the
     *     application.
     * @return the same builder instance
     */
    FinalBuilder<C> withAdditionalAllowedHeaders(String... additionalAllowedHeaders);

    /**
     * Defines additional HTTP headers that can be exposed to clients. The headers defined in {@link
     * #ALWAYS_EXPOSED_HEADERS} can always be exposed and must not be added here.
     *
     * @param additionalExposedHeaders additional HTTP headers that can be exposed.
     * @return the same builder instance
     */
    FinalBuilder<C> withAdditionalExposedHeaders(String... additionalExposedHeaders);

    CorsBundle<C> build();
  }

  public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

    private Set<String> allowedHttpMethods = new LinkedHashSet<>(DEFAULT_HTTP_METHODS);
    private final Set<String> allowedHeaders = new LinkedHashSet<>(ALWAYS_ALLOWED_HEADERS);
    private final Set<String> exposedHeaders = new LinkedHashSet<>(ALWAYS_EXPOSED_HEADERS);

    private CorsConfigProvider<C> configProvider = (c -> new CorsConfiguration());

    private Builder() {}

    private Builder(CorsConfigProvider<C> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <C1 extends Configuration> FinalBuilder<C1> withCorsConfigProvider(
        CorsConfigProvider<C1> configProvider) {
      return new Builder<>(configProvider);
    }

    @Override
    public FinalBuilder<C> withAllowedMethods(String... httpMethods) {
      this.allowedHttpMethods =
          Stream.of(httpMethods)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      return this;
    }

    @Override
    public FinalBuilder<C> withAdditionalAllowedHeaders(String... additionalAllowedHeaders) {
      Stream.of(additionalAllowedHeaders)
          .filter(Objects::nonNull)
          .forEach(this.allowedHeaders::add);
      return this;
    }

    @Override
    public FinalBuilder<C> withAdditionalExposedHeaders(String... additionalExposedHeaders) {
      Stream.of(additionalExposedHeaders)
          .filter(Objects::nonNull)
          .forEach(this.exposedHeaders::add);
      return this;
    }

    @Override
    public CorsBundle<C> build() {
      return new CorsBundle<>(configProvider, allowedHttpMethods, allowedHeaders, exposedHeaders);
    }
  }
}
