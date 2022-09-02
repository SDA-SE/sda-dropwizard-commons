package org.sdase.commons.server.opentelemetry.servlet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;

public class TracingAsyncListener implements AsyncListener {

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
    serverSpan.setAttribute(SemanticAttributes.EXCEPTION_EVENT_NAME, "Timeout exceeded");
    serverSpan.setAttribute(
        AttributeKey.longKey("http.request.timeout.ms"), event.getAsyncContext().getTimeout());
  }

  @Override
  public void onError(AsyncEvent event) {
    serverSpan.setStatus(StatusCode.ERROR);
    serverSpan.setAttribute(SemanticAttributes.EXCEPTION_EVENT_NAME, "Async servlet error");
    serverSpan.recordException(event.getThrowable());
  }

  @Override
  public void onStartAsync(AsyncEvent event) {
    // nothing to do here
  }
}
