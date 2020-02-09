package org.sdase.commons.server.opentracing;

import static java.util.Arrays.asList;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;
import io.opentracing.util.GlobalTracer;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import org.sdase.commons.server.opentracing.jaxrs.CustomServerSpanDecorator;
import org.sdase.commons.server.opentracing.jaxrs.ExceptionListener;
import org.sdase.commons.server.opentracing.logging.SpanLogsAppender;
import org.sdase.commons.server.opentracing.servlet.AdminServletSpanDecorator;
import org.sdase.commons.server.opentracing.servlet.CustomServletSpanDecorator;
import org.slf4j.LoggerFactory;

public class OpenTracingBundle implements Bundle {

  private final Tracer tracer;

  private OpenTracingBundle(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // do nothing here
  }

  @Override
  public void run(Environment environment) {
    Tracer currentTracer = tracer == null ? GlobalTracer.get() : tracer;

    registerLogAppender(currentTracer);
    registerServletFilter(currentTracer, environment);
    registerJaxRsFilters(currentTracer, environment);
  }

  private void registerLogAppender(Tracer currentTracer) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = context.getLogger(ROOT_LOGGER_NAME);
    SpanLogsAppender appender = new SpanLogsAppender(currentTracer);
    appender.start();

    rootLogger.addAppender(appender);
  }

  private void registerServletFilter(Tracer currentTracer, Environment environment) {
    List<ServletFilterSpanDecorator> decorators =
        asList(ServletFilterSpanDecorator.STANDARD_TAGS, new CustomServletSpanDecorator());
    TracingFilter filter = new TracingFilter(currentTracer, decorators, null);
    FilterRegistration.Dynamic filterRegistration =
        environment.servlets().addFilter("TracingFilter", filter);
    filterRegistration.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "*");

    // The admin endpoint only has servlet support. Tracing this allows to filter health checks and
    // metrics.
    List<ServletFilterSpanDecorator> adminDecorators =
        asList(
            ServletFilterSpanDecorator.STANDARD_TAGS,
            new CustomServletSpanDecorator(),
            new AdminServletSpanDecorator());
    TracingFilter adminFilter = new TracingFilter(currentTracer, adminDecorators, null);
    FilterRegistration.Dynamic adminFilterRegistration =
        environment.admin().addFilter("AdminTracingFilter", adminFilter);
    adminFilterRegistration.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "*");
  }

  private void registerJaxRsFilters(Tracer currentTracer, Environment environment) {
    List<ServerSpanDecorator> decorators =
        asList(ServerSpanDecorator.STANDARD_TAGS, new CustomServerSpanDecorator());

    environment.jersey().register(new ExceptionListener(currentTracer));
    environment
        .jersey()
        .register(
            new ServerTracingDynamicFeature.Builder(currentTracer)
                .withJoinExistingActiveSpan(true)
                .withDecorators(decorators)
                .build());
    Dynamic filterRegistration =
        environment.servlets().addFilter("SpanFinishingFilter", SpanFinishingFilter.class);
    filterRegistration.setAsyncSupported(true);
    filterRegistration.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), true, "*");
  }

  public static FinalBuilder builder() {
    return new Builder();
  }

  public interface FinalBuilder {

    /**
     * Explicitly sets the tracer to use. If no tracer is specified, the {@link GlobalTracer} is
     * used.
     *
     * @param tracer tracer to use.
     * @return the same builder instance
     */
    FinalBuilder withTracer(Tracer tracer);

    OpenTracingBundle build();
  }

  public static class Builder implements FinalBuilder {

    private Tracer tracer;

    private Builder() {}

    @Override
    public FinalBuilder withTracer(Tracer tracer) {
      this.tracer = tracer;
      return this;
    }

    @Override
    public OpenTracingBundle build() {
      return new OpenTracingBundle(tracer);
    }
  }
}
