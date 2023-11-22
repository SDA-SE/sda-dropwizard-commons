package org.sdase.commons.server.trace.filter;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import java.util.Optional;
import java.util.UUID;
import org.sdase.commons.shared.tracing.RequestTracing;
import org.slf4j.MDC;

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
    String token = extractTokenFromRequest(requestContext).orElse(UUID.randomUUID().toString());

    // Add token to request context so that it is available within the application
    this.addTokenToRequest(requestContext, token);

    this.addTokenToMdc(token);
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    Optional<String> token = extractTokenFromRequestProperties(requestContext);
    token.ifPresent(s -> responseContext.getHeaders().add(RequestTracing.TOKEN_HEADER, s));
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

  private void addTokenToMdc(String token) {
    if (MDC.getMDCAdapter() != null) {
      MDC.put(RequestTracing.TOKEN_MDC_KEY, token);
    }
  }

  private void addTokenToRequest(ContainerRequestContext requestContext, String token) {
    requestContext.setProperty(RequestTracing.TOKEN_ATTRIBUTE, token);
  }
}
