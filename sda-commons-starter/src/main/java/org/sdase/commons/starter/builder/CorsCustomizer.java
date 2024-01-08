package org.sdase.commons.starter.builder;

import io.dropwizard.core.Configuration;
import org.sdase.commons.server.cors.CorsBundle;

public interface CorsCustomizer<T extends Configuration> {

  /**
   * Defines the HTTP methods that are used in the application and should be allowed for cross
   * origin clients. The given {@code httpMethods} overwrite the {@link
   * CorsBundle.FinalBuilder#DEFAULT_HTTP_METHODS default methods}.
   *
   * <ul>
   *   <li>{@value jakarta.ws.rs.HttpMethod#OPTIONS}
   *   <li>{@value jakarta.ws.rs.HttpMethod#HEAD}
   *   <li>{@value jakarta.ws.rs.HttpMethod#GET}
   *   <li>{@value jakarta.ws.rs.HttpMethod#DELETE}
   *   <li>{@value jakarta.ws.rs.HttpMethod#POST}
   *   <li>{@value jakarta.ws.rs.HttpMethod#PUT}
   *   <li>{@code "PATCH"}
   * </ul>
   *
   * @param httpMethods all HTTP methods that will be allowed by the CORS filter.
   * @return the same builder instance
   */
  PlatformBundleBuilder<T> withCorsAllowedMethods(String... httpMethods);

  /**
   * Defines additional HTTP headers that clients may send to the application. The headers defined
   * in {@link CorsBundle.FinalBuilder#ALWAYS_ALLOWED_HEADERS} are always allowed to be send and
   * must not be added here.
   *
   * @param additionalAllowedHeaders additional HTTP headers that clients may send to the
   *     application.
   * @return the same builder instance
   */
  PlatformBundleBuilder<T> withCorsAdditionalAllowedHeaders(String... additionalAllowedHeaders);

  /**
   * Defines additional HTTP headers that can be exposed to clients. The headers defined in {@link
   * CorsBundle.FinalBuilder#ALWAYS_EXPOSED_HEADERS} can always be exposed and must not be added
   * here.
   *
   * @param additionalExposedHeaders additional HTTP headers that can be exposed.
   * @return the same builder instance
   */
  PlatformBundleBuilder<T> withCorsAdditionalExposedHeaders(String... additionalExposedHeaders);
}
