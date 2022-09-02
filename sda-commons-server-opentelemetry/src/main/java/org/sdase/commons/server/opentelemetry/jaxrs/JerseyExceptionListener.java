package org.sdase.commons.server.opentelemetry.jaxrs;

import static org.glassfish.jersey.server.monitoring.RequestEvent.Type.ON_EXCEPTION;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

public class JerseyExceptionListener implements ApplicationEventListener {

  @Override
  public void onEvent(ApplicationEvent event) {
    // nothing to do
  }

  @Override
  public RequestEventListener onRequest(RequestEvent requestEvent) {
    if (requestEvent.getType() == RequestEvent.Type.START) {
      return new ExceptionRequestEventListener();
    }
    return null;
  }

  static class ExceptionRequestEventListener implements RequestEventListener {
    @Override
    public void onEvent(RequestEvent event) {
      if (event.getType().equals(ON_EXCEPTION)) {
        Span span = Span.current();

        if (span != null) {
          span.setStatus(StatusCode.ERROR, event.getException().getMessage());
          span.recordException(event.getException());
        }
      }
    }
  }
}
