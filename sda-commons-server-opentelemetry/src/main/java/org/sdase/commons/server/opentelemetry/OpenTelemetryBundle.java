package org.sdase.commons.server.opentelemetry;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import org.sdase.commons.server.opentelemetry.autoconfig.SdaConfigPropertyProvider;
import org.sdase.commons.server.opentelemetry.jaxrs.JerseyExceptionListener;
import org.sdase.commons.server.opentelemetry.jaxrs.ServerTracingFilter;
import org.sdase.commons.server.opentelemetry.servlet.TracingFilter;

public class OpenTelemetryBundle implements ConfiguredBundle<Configuration> {
  private final OpenTelemetry openTelemetry;
  private final Pattern excludedUrlPatterns;

  public OpenTelemetryBundle(OpenTelemetry openTelemetry, Pattern excludedUrlPatterns) {
    this.openTelemetry = openTelemetry;
    this.excludedUrlPatterns = excludedUrlPatterns;
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // Initialize a telemetry instance if not set.
    OpenTelemetry currentTelemetryInstance =
        this.openTelemetry == null ? GlobalOpenTelemetry.get() : this.openTelemetry;

    registerJaxrsTracer(environment.jersey());
    registerServletTracer(environment, currentTelemetryInstance);
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // nothing here
  }

  public void registerServletTracer(Environment environment, OpenTelemetry openTelemetry) {
    TracingFilter tracingFilter = new TracingFilter(openTelemetry, excludedUrlPatterns);

    FilterRegistration.Dynamic filterRegistration =
        environment.servlets().addFilter("TracingFilter", tracingFilter);
    filterRegistration.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "*");
  }

  public void registerJaxrsTracer(JerseyEnvironment jersey) {
    jersey.register(JerseyExceptionListener.class);
    jersey.register(new ServerTracingFilter());
  }

  private static OpenTelemetry bootstrapConfiguredTelemetrySdk() {
    return AutoConfiguredOpenTelemetrySdk.builder()
        .addPropertiesSupplier(SdaConfigPropertyProvider::getProperties)
        .setResultAsGlobal(true)
        .build()
        .getOpenTelemetrySdk();
  }

  public static InitialBuilder builder() {
    return new Builder();
  }

  public interface InitialBuilder {
    /**
     * Specifies a custom telemetry instance to use. If no instance is specified, the {@link
     * GlobalOpenTelemetry} is used.
     *
     * @param openTelemetry The telemetry instance to use
     * @return the same builder
     */
    FinalBuilder withTelemetryInstance(OpenTelemetry openTelemetry);

    /**
     * Enables the bundle to setup an auto configured instance of OpenTelemetry Sdk, and register it
     * as Global.
     *
     * @return the same builder
     */
    FinalBuilder withAutoConfiguredTelemetryInstance();
  }

  public interface FinalBuilder {
    /**
     * Specifies a pattern where all traces for the matching urls will be suppressed. By default all
     * requests with any url is traced.
     *
     * @param pattern The url pattern to be excluded.
     * @return the same builder
     */
    FinalBuilder withExcludedUrlsPattern(Pattern pattern);

    OpenTelemetryBundle build();
  }

  public static class Builder implements FinalBuilder, InitialBuilder {

    private Supplier<OpenTelemetry> openTelemetryProvider;
    private Pattern excludedUrlPatterns;

    private Builder() {}

    @Override
    public FinalBuilder withTelemetryInstance(OpenTelemetry openTelemetry) {
      this.openTelemetryProvider = () -> openTelemetry;
      return this;
    }

    @Override
    public FinalBuilder withExcludedUrlsPattern(Pattern excludedUrlPatterns) {
      this.excludedUrlPatterns = excludedUrlPatterns;
      return this;
    }

    @Override
    public FinalBuilder withAutoConfiguredTelemetryInstance() {
      this.openTelemetryProvider = OpenTelemetryBundle::bootstrapConfiguredTelemetrySdk;
      return this;
    }

    @Override
    public OpenTelemetryBundle build() {
      return new OpenTelemetryBundle(openTelemetryProvider.get(), excludedUrlPatterns);
    }
  }
}
