package org.sdase.commons.server.opentracing.servlet;

import static java.util.Collections.list;
import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_REQUEST_HEADERS;
import static org.sdase.commons.server.opentracing.tags.TagUtils.HTTP_RESPONSE_HEADERS;

import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import org.sdase.commons.server.opentracing.tags.TagUtils;

public class CustomServletSpanDecorator implements ServletFilterSpanDecorator {

  @Override
  public void onRequest(HttpServletRequest httpServletRequest, Span span) {
    MultivaluedHashMap<String, String> headers = extractRequestHeaders(httpServletRequest);

    span.setTag(HTTP_REQUEST_HEADERS, TagUtils.convertHeadersToString(headers));
  }

  @Override
  public void onResponse(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Span span) {
    MultivaluedHashMap<String, String> headers = extractResponseHeaders(httpServletResponse);

    span.setTag(HTTP_RESPONSE_HEADERS, TagUtils.convertHeadersToString(headers));
  }

  @Override
  public void onError(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      Throwable exception,
      Span span) {
    // No special tags
  }

  @Override
  public void onTimeout(
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse,
      long timeout,
      Span span) {
    // No special tags
  }

  private MultivaluedHashMap<String, String> extractRequestHeaders(
      HttpServletRequest httpServletRequest) {
    MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    Enumeration<String> headersNames = httpServletRequest.getHeaderNames();
    while (headersNames.hasMoreElements()) {
      String name = headersNames.nextElement();
      headers.addAll(name, list(httpServletRequest.getHeaders(name)));
    }
    return headers;
  }

  private MultivaluedHashMap<String, String> extractResponseHeaders(
      HttpServletResponse httpServletResponse) {
    MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    for (String name : httpServletResponse.getHeaderNames()) {
      headers.addAll(name, new ArrayList<>(httpServletResponse.getHeaders(name)));
    }
    return headers;
  }
}
