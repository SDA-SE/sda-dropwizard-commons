package org.sdase.commons.server.trace.filter;

import java.util.Optional;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import org.sdase.commons.shared.tracing.RequestTracing;
import org.sdase.commons.shared.tracing.TraceContext;

/**
 * A request filter land response filter that detects, optionally generates if not existing and
 * provides the trace token in requests.
 */
@PreMatching // No matching is required, should happen as early as possible
public class TraceTokenServerFilter implements ContainerRequestFilter, ContainerResponseFilter {

  @Override
  public void filter(ContainerRequestContext requestContext) {

    // In case of OPTIONS, no headers can be provided. Usually OPTION requests are from browsers for
    // CORS.
    if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) {
      return;
    }

    // Get the HTTP trace token header from the request
    Optional<String> token = extractTokenFromRequest(requestContext);
    if (token.isPresent()) {
      TraceContext.storeTraceToken(token.get());
    } else {
      token = Optional.of(TraceContext.createNewTraceToken());
    }

    // Add token to request context so that it is available within the application
    this.addTokenToRequest(requestContext, token.get());
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    Optional<String> token = extractTokenFromRequestProperties(requestContext);
    token.ifPresent(s -> responseContext.getHeaders().add(RequestTracing.TOKEN_HEADER, s));
    TraceContext.clear();
  }

  private Optional<String> extractTokenFromRequestProperties(
      ContainerRequestContext requestContext) {
    String requestToken = (String) requestContext.getProperty(RequestTracing.TOKEN_ATTRIBUTE);
    if (requestToken == null || requestToken.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(requestToken);
  }

  private Optional<String> extractTokenFromRequest(ContainerRequestContext requestContext) {
    String requestToken = requestContext.getHeaderString(RequestTracing.TOKEN_HEADER);
    if (requestToken == null || requestToken.trim().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(requestToken);
  }

  private void addTokenToRequest(ContainerRequestContext requestContext, String token) {
    requestContext.setProperty(RequestTracing.TOKEN_ATTRIBUTE, token);
  }
}
