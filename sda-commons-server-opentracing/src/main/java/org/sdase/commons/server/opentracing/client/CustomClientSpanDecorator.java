package org.sdase.commons.server.opentracing.client;

import static org.sdase.commons.server.opentracing.filter.TagUtils.HTTP_REQUEST_HEADERS;
import static org.sdase.commons.server.opentracing.filter.TagUtils.HTTP_RESPONSE_HEADERS;
import static org.sdase.commons.server.opentracing.filter.TagUtils.convertHeadersToString;

import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

public class CustomClientSpanDecorator implements ClientSpanDecorator {

  @Override
  public void decorateRequest(ClientRequestContext requestContext, Span span) {
    span.setTag(HTTP_REQUEST_HEADERS.getKey(), convertHeadersToString(requestContext.getHeaders()));
  }

  @Override
  public void decorateResponse(ClientResponseContext responseContext, Span span) {
    span.setTag(
        HTTP_RESPONSE_HEADERS.getKey(), convertHeadersToString(responseContext.getHeaders()));
  }
}
