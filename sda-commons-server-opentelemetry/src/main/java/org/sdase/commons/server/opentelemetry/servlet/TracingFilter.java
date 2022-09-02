package org.sdase.commons.server.opentelemetry.servlet;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sdase.commons.server.opentelemetry.decorators.ServerSpanDecorator;

public class TracingFilter implements Filter {
  private static final String INSTRUMENTATION_NAME = "sda-commons-servlet";
  private final OpenTelemetry openTelemetry;
  private final Pattern skipPattern;

  public TracingFilter(OpenTelemetry openTelemetry, Pattern skipPattern) {
    this.openTelemetry = openTelemetry;
    this.skipPattern = skipPattern;
  }

  HttpRequestKeyMapGetter getter = new HttpRequestKeyMapGetter();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
  }

  @SuppressWarnings("java:S1181")
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // 1 - skip tracing for excluded patterns
    if (!isTraced(httpRequest)) {
      chain.doFilter(httpRequest, httpResponse);
      return;
    }

    // 2 - check if request is already traced
    if (Span.current().getSpanContext().isValid()) {
      chain.doFilter(request, response);
      return;
    }

    // 3 - trace
    Context context = getCurrentContextOrNew(httpRequest);

    //  setup context
    try (Scope ignored1 = context.makeCurrent()) {
      Span serverSpan =
          openTelemetry
              .getTracer(INSTRUMENTATION_NAME)
              .spanBuilder(
                  getRequestSpanName(
                      httpRequest)) // A more specific name is provided by the jaxrs filter
              .setSpanKind(SpanKind.SERVER)
              .startSpan();

      try (Scope ignored = serverSpan.makeCurrent()) {
        // Add the attributes defined in the Semantic Conventions
        ServerSpanDecorator.decorateRequest(httpRequest, serverSpan);

        chain.doFilter(request, response);

        // this needs to be added after invoking the filter chain
        if (!httpRequest.isAsyncStarted()) {
          // add response headers to current span
          ServerSpanDecorator.decorateResponse(httpResponse, serverSpan);
        }

      } catch (Throwable e) {
        // include error in the current span
        serverSpan.setStatus(StatusCode.ERROR);
        serverSpan.recordException(e);
        // populate the caught exception
        throw e;
      } finally {
        if (httpRequest.isAsyncStarted()) {
          httpRequest.getAsyncContext().addListener(new TracingAsyncListener(serverSpan));
        } else {
          // If not async, then need to explicitly finish the span associated with the scope.
          // finish the current span
          serverSpan.end();
        }
      }
    }
  }

  @Override
  public void destroy() {
    Filter.super.destroy();
  }

  private String getRequestSpanName(HttpServletRequest request) {
    return String.format("%s %s", request.getMethod(), request.getRequestURI());
  }

  protected boolean isTraced(HttpServletRequest httpServletRequest) {
    // skip URLs matching skip pattern
    // e.g. pattern is defined as '/metrics/prometheus' then URL
    // 'http://localhost:5000/metrics/prometheus' won't be traced
    if (skipPattern != null) {
      int contextLength =
          httpServletRequest.getContextPath() == null
              ? 0
              : httpServletRequest.getContextPath().length();
      String url = httpServletRequest.getRequestURI().substring(contextLength);
      return !skipPattern.matcher(url).matches();
    }

    return true;
  }

  private Context getCurrentContextOrNew(HttpServletRequest request) {
    return openTelemetry
        .getPropagators()
        .getTextMapPropagator()
        .extract(Context.current(), request, getter);
  }
}
