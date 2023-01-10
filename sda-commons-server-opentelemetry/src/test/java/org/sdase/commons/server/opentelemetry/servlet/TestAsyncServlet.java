package org.sdase.commons.server.opentelemetry.servlet;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("java:S2925")
public class TestAsyncServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    if (req.getRequestURI().contains("/error")) {
      throwError();
    } else if (req.getRequestURI().contains("/timeout")) {
      throwTimeout(req);
    } else {
      createInternalSpan(req);
    }
  }

  private void throwError() throws IOException {
    throw new IOException("Error while doing something.");
  }

  private void throwTimeout(HttpServletRequest req) {
    final AsyncContext ctx = req.startAsync();
    // a short timeout duration
    ctx.setTimeout(100);
    ctx.start(
        () -> {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            // do nothing
          }
        });
  }

  private void createInternalSpan(HttpServletRequest req) {
    final AsyncContext ctx = req.startAsync();
    ctx.start(Context.current().wrap(() -> doSomething(ctx)));
    ctx.complete();
  }

  private void doSomething(AsyncContext ctx) {
    Span span =
        GlobalOpenTelemetry.get()
            .getTracer("async-servlet-test")
            .spanBuilder("async-process-test")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      // do nothing
    } finally {
      span.end();
    }
  }
}
