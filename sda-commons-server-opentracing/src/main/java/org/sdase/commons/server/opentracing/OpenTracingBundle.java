package org.sdase.commons.server.opentracing;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;
import io.opentracing.util.GlobalTracer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import org.sdase.commons.server.opentracing.filter.CustomServerSpanDecorator;
import org.sdase.commons.server.opentracing.filter.ExceptionListener;

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

    List<ServerSpanDecorator> serverDecorators = new ArrayList<>();
    serverDecorators.add(ServerSpanDecorator.STANDARD_TAGS);
    serverDecorators.add(new CustomServerSpanDecorator());

    environment.jersey().register(new ExceptionListener(currentTracer));
    environment
        .jersey()
        .register(
            new ServerTracingDynamicFeature.Builder(currentTracer)
                .withJoinExistingActiveSpan(true)
                .withDecorators(serverDecorators)
                .build());
    Dynamic filterRegistration =
        environment.servlets().addFilter("SpanFinishingFilter", SpanFinishingFilter.class);
    filterRegistration.setAsyncSupported(true);
    filterRegistration.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "*");
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
