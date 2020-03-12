package org.sdase.commons.server.auth.filter;

import static io.opentracing.tag.Tags.COMPONENT;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import org.sdase.commons.server.auth.error.JwtAuthException;

/**
 * A dropwizard authentication filter using JSON Web Token (JWT). The filter checks if JWT (Json Web
 * Token) sent by the user is a valid signed JWT to access a protected route or resource, the user
 * agent should send the JWT, typically in the Authorization header using the Bearer schema. The
 * content of the header should look like the following: {@code Authorization: Bearer <token>}. If
 * acceptAnonymous is true, the auth filter accepts non-existing tokens and skips the initial
 * authorization.
 *
 * @param <P> The type of the principal.
 */
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter<P extends Principal> extends AuthFilter<Optional<String>, P> {

  private static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";
  private static final String AUTHENTICATED = "authenticated";

  private boolean acceptAnonymous;
  private Tracer tracer;

  private JwtAuthFilter() {}

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Span span =
        tracer
            .buildSpan("validateToken")
            .withTag(COMPONENT, "JwtAuthFilter")
            .withTag(AUTHENTICATED, false)
            .start();

    try (Scope ignored = tracer.scopeManager().activate(span)) {
      final MultivaluedMap<String, String> headers = requestContext.getHeaders();
      final String jwt = extractAuthorizationToken(headers);

      // validates the token and throws exception if invalid or expired
      boolean authenticated =
          authenticate(requestContext, Optional.ofNullable(jwt), SecurityContext.BASIC_AUTH);

      span.setTag(AUTHENTICATED, authenticated);

      if (!acceptAnonymous && !authenticated) {
        throw new JwtAuthException("Credentials are required to access this resource.");
      }
    } finally {
      span.finish();
    }
  }

  // builder

  /**
   * Builder for {@link JwtAuthFilter}.
   *
   * <p>An {@link Authenticator} must be provided during the building process.
   *
   * @param <P> the type of the principal
   */
  public static class Builder<P extends Principal>
      extends AuthFilterBuilder<Optional<String>, P, JwtAuthFilter<P>> {

    private boolean acceptAnonymous;
    private Tracer tracer = NoopTracerFactory.create();

    @Override
    protected JwtAuthFilter<P> newInstance() {
      return new JwtAuthFilter<>();
    }

    public Builder<P> setAcceptAnonymous(boolean acceptAnonymous) {
      this.acceptAnonymous = acceptAnonymous;
      return this;
    }

    public Builder<P> withTracer(Tracer tracer) {
      this.tracer = tracer;
      return this;
    }

    @Override
    public JwtAuthFilter<P> buildAuthFilter() {
      JwtAuthFilter<P> jwtAuthFilter = super.buildAuthFilter();

      jwtAuthFilter.acceptAnonymous = acceptAnonymous;
      jwtAuthFilter.tracer = tracer;

      return jwtAuthFilter;
    }
  }

  private String extractAuthorizationToken(MultivaluedMap<String, String> headers) {

    final List<String> authorization = headers.get(HttpHeaders.AUTHORIZATION);
    if (authorization == null || authorization.isEmpty()) {
      return null;
    }
    String authorizationHeader = authorization.get(0);
    if (!authorizationHeader.contains(AUTHENTICATION_SCHEME_BEARER)) {
      return null;
    }
    return authorizationHeader.replaceFirst(AUTHENTICATION_SCHEME_BEARER + " ", "");
  }
}
