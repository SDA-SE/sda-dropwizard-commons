package org.sdase.commons.server.opentracing.jaxrs;

import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_REQUEST_HEADERS;
import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_RESPONSE_HEADERS;
import static org.sdase.commons.server.opentracing.tags.TagUtils.convertHeadersToString;

import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

public class CustomServerSpanDecorator implements ServerSpanDecorator {

  @Override
  public void decorateRequest(ContainerRequestContext requestContext, Span span) {
    span.setTag(HTTP_REQUEST_HEADERS.getKey(), convertHeadersToString(requestContext.getHeaders()));
  }

  @Override
  public void decorateResponse(ContainerResponseContext responseContext, Span span) {
    span.setTag(
        HTTP_RESPONSE_HEADERS.getKey(), convertHeadersToString(responseContext.getHeaders()));
  }
}
