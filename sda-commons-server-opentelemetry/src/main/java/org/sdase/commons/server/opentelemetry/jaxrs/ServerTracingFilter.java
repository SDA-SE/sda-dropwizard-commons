package org.sdase.commons.server.opentelemetry.jaxrs;

import io.opentelemetry.api.trace.Span;
import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.sdase.commons.server.opentelemetry.decorators.ServerSpanDecorator;
import org.slf4j.MDC;

/**
 * This is only responsible for adding additional attributes to the server span create by a servlet
 * instrumentation, and update it's name with a better one.
 */
@Priority(Priorities.HEADER_DECORATOR)
public class ServerTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) {
    Span current = Span.current();
    // Skip if there is no span created by a the servlet instrumentation.
    if (!current.getSpanContext().isValid()) {
      return;
    }
    // avoid creating a new span, instead decorate the current span.
    ServerSpanDecorator.decorateRequest(requestContext, current);
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    Span current = Span.current();
    // Skip if there is no span created by a the servlet instrumentation.
    if (!current.getSpanContext().isValid()) {
      return;
    }
    addTraceIdToResponseAndMdc(responseContext, current);
  }

  private static void addTraceIdToResponseAndMdc(
      ContainerResponseContext responseContext, Span current) {
    ServerSpanDecorator.decorateResponse(responseContext, current);
    responseContext.getHeaders().add("TraceID", current.getSpanContext().getTraceId());
    MDC.put("TraceID", current.getSpanContext().getTraceId());
  }
}
