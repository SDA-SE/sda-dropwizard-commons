package org.sdase.commons.server.opentelemetry.decorators;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.server.opentelemetry.jaxrs.JerseySpanNameProvider;

public class ServerSpanDecorator {
  private ServerSpanDecorator() {}

  public static void decorateRequest(ContainerRequestContext requestContext, Span span) {
    span.setAttribute(
        HeadersUtils.HTTP_REQUEST_HEADERS,
        HeadersUtils.convertHeadersToString(requestContext.getHeaders()));

    String route =
        Objects.requireNonNull(new JerseySpanNameProvider().get(requestContext.getRequest()));
    updateSpanName(requestContext.getMethod(), route, span);
    // Add the attributes defined in the Semantic Conventions
    span.setAttribute(SemanticAttributes.HTTP_ROUTE, route);
  }

  public static void decorateRequest(HttpServletRequest request, Span span) {
    span.setAttribute(SemanticAttributes.HTTP_METHOD, request.getMethod());
    span.setAttribute(SemanticAttributes.HTTP_SCHEME, request.getScheme());
    span.setAttribute(SemanticAttributes.HTTP_HOST, request.getRemoteHost());
    span.setAttribute(SemanticAttributes.HTTP_TARGET, request.getContextPath());
    span.setAttribute(SemanticAttributes.HTTP_URL, request.getRequestURI());
  }

  public static void decorateResponse(ContainerResponseContext responseContext, Span span) {
    span.setAttribute(
        HeadersUtils.HTTP_RESPONSE_HEADERS,
        HeadersUtils.convertHeadersToString(responseContext.getHeaders()));
  }

  public static void decorateResponse(HttpServletResponse response, Span span) {
    span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, response.getStatus());
  }

  private static void updateSpanName(String method, String route, Span span) {
    if (StringUtils.isBlank(route) || StringUtils.isBlank(method)) {
      return;
    }
    span.updateName(String.format("%s %s", method, route));
  }
}
