package org.sdase.commons.server.opentracing.filter;

import static org.glassfish.jersey.server.monitoring.RequestEvent.Type.ON_EXCEPTION;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

public class ExceptionListener implements ApplicationEventListener {
  private final Tracer tracer;

  public ExceptionListener(Tracer tracer) {
    this.tracer = tracer;
  }

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

  class ExceptionRequestEventListener implements RequestEventListener {
    @Override
    public void onEvent(RequestEvent event) {
      if (event.getType().equals(ON_EXCEPTION)) {
        Span span = tracer.activeSpan();

        if (span != null) {
          span.setTag(Tags.ERROR.getKey(), true);
          span.log(mapExceptionToFields(event.getException()));
        }
      }
    }

    private Map<String, Object> mapExceptionToFields(Throwable throwable) {
      Map<String, Object> fields = new HashMap<>();
      fields.put("event", "error");
      fields.put("error.kind", "Exception");
      fields.put("error.object", throwable);
      fields.put("message", throwable.getMessage());
      fields.put("stack", mapStackToString(throwable));
      return fields;
    }

    private String mapStackToString(Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    }
  }
}
