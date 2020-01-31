package org.sdase.commons.server.opentracing.jaxrs;

import static io.opentracing.log.Fields.ERROR_KIND;
import static io.opentracing.log.Fields.ERROR_OBJECT;
import static io.opentracing.log.Fields.EVENT;
import static io.opentracing.log.Fields.MESSAGE;
import static io.opentracing.log.Fields.STACK;
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
      fields.put(EVENT, "error");
      fields.put(ERROR_KIND, throwable.getClass().getName());
      fields.put(ERROR_OBJECT, throwable);
      fields.put(MESSAGE, throwable.getMessage());
      fields.put(STACK, mapStackToString(throwable));
      return fields;
    }

    private String mapStackToString(Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      return sw.toString();
    }
  }
}
