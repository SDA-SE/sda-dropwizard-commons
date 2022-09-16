package org.sdase.commons.server.opentelemetry.servlet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

public class TracingAsyncListener implements AsyncListener {
  public static final AttributeKey<Long> REQUEST_TIMEOUT_ATTRIBUTE_KEY =
      AttributeKey.longKey("http.request.timeout.ms");

  private final Span serverSpan;

  public TracingAsyncListener(Span serverSpan) {
    this.serverSpan = serverSpan;
  }

  @Override
  public void onComplete(AsyncEvent event) {
    serverSpan.end();
  }

  @Override
  public void onTimeout(AsyncEvent event) {
    serverSpan.setStatus(StatusCode.ERROR);
    serverSpan.setAttribute(REQUEST_TIMEOUT_ATTRIBUTE_KEY, event.getAsyncContext().getTimeout());
  }

  @Override
  public void onError(AsyncEvent event) {
    serverSpan.setStatus(StatusCode.ERROR);
    serverSpan.recordException(event.getThrowable());
  }

  @Override
  public void onStartAsync(AsyncEvent event) {
    // nothing to do here
  }
}
