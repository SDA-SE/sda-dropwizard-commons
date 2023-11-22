package org.sdase.commons.server.opentelemetry.jaxrs;

import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import org.sdase.commons.server.opentelemetry.decorators.ServerSpanDecorator;

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
    ServerSpanDecorator.decorateResponse(responseContext, current);
  }
}
