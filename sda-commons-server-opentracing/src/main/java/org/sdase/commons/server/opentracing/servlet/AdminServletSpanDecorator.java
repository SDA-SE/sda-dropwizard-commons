package org.sdase.commons.server.opentracing.servlet;

import static io.opentracing.tag.Tags.SAMPLING_PRIORITY;

import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminServletSpanDecorator implements ServletFilterSpanDecorator {

  private static final String JAEGER_DEBUG_ID = "jaeger-debug-id";

  @Override
  public void onRequest(HttpServletRequest httpServletRequest, Span span) {
    // Avoid that the admin endpoint's spans are sampled. This is implementation specific and not
    // guaranteed, but Jaeger avoids sampling them and their children.
    // However, still keep the option to sample if the Jaeger debug id is present.
    if (httpServletRequest.getHeader(JAEGER_DEBUG_ID) == null) {
      span.setTag(SAMPLING_PRIORITY, 0);
    }
  }

  @Override
  public void onResponse(
      HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Span span) {
    // No special tags
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
}
