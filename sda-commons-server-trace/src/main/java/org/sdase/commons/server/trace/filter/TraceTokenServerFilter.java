package org.sdase.commons.server.trace.filter;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import org.sdase.commons.shared.tracing.RequestTracing;
import org.sdase.commons.shared.tracing.TraceTokenContext;

/**
 * A request filter land response filter that detects, optionally generates if not existing and
 * provides the trace token in requests.
 */
@PreMatching // No matching is required, should happen as early as possible
public class TraceTokenServerFilter implements ContainerRequestFilter, ContainerResponseFilter {

  private static final String TRACE_TOKEN_CONTEXT_KEY =
      TraceTokenServerFilter.class.getName() + "_TRACE_TOKEN_CONTEXT";

  @Override
  public void filter(ContainerRequestContext requestContext) {

    // In case of OPTIONS, no headers can be provided. Usually OPTION requests are from browsers for
    // CORS.
    if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) {
      return;
    }

    // Get the HTTP trace token header from the request
    var incomingTraceToken =
        requestContext.getHeaderString(TraceTokenContext.TRACE_TOKEN_HTTP_HEADER_NAME);
    var traceTokenContext =
        TraceTokenContext.continueSynchronousTraceTokenContext(incomingTraceToken);
    requestContext.setProperty(TRACE_TOKEN_CONTEXT_KEY, traceTokenContext);

    // deprecated: Add token to request context so that it is available within the application
    this.addTokenToRequest(requestContext, traceTokenContext.get());
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (requestContext.getProperty(TRACE_TOKEN_CONTEXT_KEY)
        instanceof TraceTokenContext traceTokenContext) {
      responseContext
          .getHeaders()
          .add(TraceTokenContext.TRACE_TOKEN_HTTP_HEADER_NAME, traceTokenContext.get());
      traceTokenContext.closeIfCreated();
    }
  }

  /**
   * @deprecated the trace token will not be stored in the request context in the future, check
   *     {@link TraceTokenContext} to obtain the current trace token.
   */
  @Deprecated(forRemoval = true)
  private void addTokenToRequest(ContainerRequestContext requestContext, String token) {
    requestContext.setProperty(RequestTracing.TOKEN_ATTRIBUTE, token);
  }
}
