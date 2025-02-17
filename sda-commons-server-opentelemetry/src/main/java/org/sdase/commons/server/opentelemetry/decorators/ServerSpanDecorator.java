package org.sdase.commons.server.opentelemetry.decorators;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import java.util.Objects;
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
    span.setAttribute(HttpAttributes.HTTP_ROUTE, route);
  }

  public static void decorateRequest(HttpServletRequest request, Span span) {
    span.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, request.getMethod());
    span.setAttribute(UrlAttributes.URL_SCHEME, request.getScheme());
    span.setAttribute(
        ServerAttributes.SERVER_ADDRESS,
        String.format("%s:%s", request.getRemoteHost(), request.getServerPort()));
    span.setAttribute(UrlAttributes.URL_FULL, request.getRequestURI());
  }

  public static void decorateResponse(ContainerResponseContext responseContext, Span span) {
    span.setAttribute(
        HeadersUtils.HTTP_RESPONSE_HEADERS,
        HeadersUtils.convertHeadersToString(responseContext.getHeaders()));
  }

  public static void decorateResponse(HttpServletResponse response, Span span) {
    span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, response.getStatus());
  }

  private static void updateSpanName(String method, String route, Span span) {
    if (StringUtils.isBlank(route) || StringUtils.isBlank(method)) {
      return;
    }
    span.updateName(String.format("%s %s", method, route));
  }
}
