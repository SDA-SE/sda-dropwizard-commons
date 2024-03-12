package org.sdase.commons.client.jersey.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import org.sdase.commons.shared.tracing.TraceTokenContext;

/**
 * The {@code TraceTokenClientFilter} adds a trace token to client requests. If existing, the trace
 * token is retrieved from the incoming request. If not existing, a new token is generated and added
 * to MDC and to the request as header.
 */
public class TraceTokenClientFilter implements ClientRequestFilter, ClientResponseFilter {

  private static final String TRACE_TOKEN_CONTEXT =
      TraceTokenClientFilter.class.getName() + "_TRACE_TOKEN_CONTEXT";

  @Override
  public void filter(ClientRequestContext requestContext) {
    var traceTokenContext = TraceTokenContext.getOrCreateTraceTokenContext();
    String traceToken = traceTokenContext.get();
    requestContext.getHeaders().add(TraceTokenContext.TRACE_TOKEN_HTTP_HEADER_NAME, traceToken);
    requestContext.setProperty(TRACE_TOKEN_CONTEXT, traceTokenContext);
  }

  // Note: This method is only called when the server responds. In case of timeouts or other errors
  // without response from the server, it may happen that a Trace-Token context that has been
  // initially created above, is not cleaned up. Usually such a Thread will terminate and the
  // Trace-Token context disappears. In rare cases the Tread may continue running with the
  // Trace-Token context created above.
  // This will not happen, if a Trace-Token context already existed when the client request filtered
  // started. In this case the context will be closed by the caller of the client.
  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
    if (requestContext.getProperty(TRACE_TOKEN_CONTEXT)
        instanceof TraceTokenContext traceTokenContext) {
      traceTokenContext.closeIfCreated();
    }
  }
}
