package org.sdase.commons.server.opentelemetry.servlet;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.eclipse.jetty.server.AsyncContextState;
import org.eclipse.jetty.server.HttpChannel;
import org.junit.platform.commons.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("java:S2925")
public class TestAsyncServlet extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(TestAsyncServlet.class);

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
    LOG.warn("Request {} received in Thread {}", req, Thread.currentThread());
    final AsyncContext ctx = req.startAsync();
    // a short timeout duration
    ctx.setTimeout(100);
    ctx.start(
        () -> {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            // do nothing
          } finally {
            ctx.complete();
          }
        });
  }

  private void createInternalSpan(HttpServletRequest req) {
    final AsyncContext ctx = req.startAsync();
    informForFlakyTest(ctx);
    ctx.start(Context.current().wrap(() -> doSomething(ctx)));
  }

  private void doSomething(AsyncContext ctx) {
    Span span =
        GlobalOpenTelemetry.get()
            .getTracer("async-servlet-test")
            .spanBuilder("async-process-test")
            .startSpan();
    try (Scope ignored = span.makeCurrent()) {
      LOG.warn("Request handled in Thread {} in AsyncContext {}", Thread.currentThread(), ctx);
    } finally {
      span.end();
      ctx.complete();
    }
  }

  /**
   * get more info for flaky test, see PLP-932
   *
   * @param ctx the current context handling the request asynchronously
   */
  private static void informForFlakyTest(AsyncContext ctx) {
    if (ctx instanceof AsyncContextState) {
      var httpChannel = ((AsyncContextState) ctx).getHttpChannel();
      var executorTry =
          ReflectionUtils.tryToReadFieldValue(HttpChannel.class, "_executor", httpChannel);
      executorTry
          .ifSuccess(
              executor ->
                  LOG.warn(
                      "Using Executor {} of type {} in HttpChannel {}",
                      executor,
                      executor.getClass(),
                      httpChannel))
          .ifFailure(
              exception ->
                  LOG.warn(
                      "Could not extract executor from HttpChannel {}", httpChannel, exception));
    }
  }
}
