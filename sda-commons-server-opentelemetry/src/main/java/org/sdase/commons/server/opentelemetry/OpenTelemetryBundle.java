package org.sdase.commons.server.opentelemetry;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

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
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.sdase.commons.server.opentelemetry.autoconfig.SdaConfigPropertyProvider;
import org.sdase.commons.server.opentelemetry.jaxrs.JerseyExceptionListener;
import org.sdase.commons.server.opentelemetry.jaxrs.ServerTracingFilter;
import org.sdase.commons.server.opentelemetry.servlet.TracingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenTelemetryBundle implements ConfiguredBundle<Configuration> {
  private static final Logger LOG = LoggerFactory.getLogger(OpenTelemetryBundle.class);
  private static final String TRACING_DISABLED = "TRACING_DISABLED";
  private static final String JAEGER_SAMPLER_TYPE = "JAEGER_SAMPLER_TYPE";
  private static final String JAEGER_SAMPLER_PARAM = "JAEGER_SAMPLER_PARAM";
  private final Supplier<OpenTelemetry> openTelemetryProvider;
  private final Pattern excludedUrlPatterns;

  public OpenTelemetryBundle(
      Supplier<OpenTelemetry> openTelemetryProvider, Pattern excludedUrlPatterns) {
    this.openTelemetryProvider = openTelemetryProvider;
    this.excludedUrlPatterns = excludedUrlPatterns;
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    OpenTelemetry openTelemetry =
        isOpenTelemetryDisabled() ? setupNoopOpenTelemetry() : openTelemetryProvider.get();

    // add server instrumentations
    registerJaxrsTracer(environment.jersey());
    registerServletTracer(environment, openTelemetry);
    registerAdminTracer(environment, openTelemetry);

    environment.lifecycle().manage(onShutdown(GlobalOpenTelemetry::resetForTest));
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

  public void registerAdminTracer(Environment environment, OpenTelemetry openTelemetry) {
    TracingFilter adminTracingFilter = new TracingFilter(openTelemetry, excludedUrlPatterns);

    FilterRegistration.Dynamic adminFilterRegistration =
        environment.admin().addFilter("AdminTracingFilter", adminTracingFilter);
    adminFilterRegistration.addMappingForUrlPatterns(
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC), false, "*");
  }

  public void registerJaxrsTracer(JerseyEnvironment jersey) {
    jersey.register(JerseyExceptionListener.class);
    jersey.register(new ServerTracingFilter());
  }

  private static OpenTelemetry bootstrapConfiguredTelemetrySdk() {
    LOG.info("No OpenTelemetry sdk instance is provided, using autoConfigured instance.");
    return AutoConfiguredOpenTelemetrySdk.builder()
        .addPropertiesSupplier(SdaConfigPropertyProvider::getProperties)
        .setResultAsGlobal(true)
        .build()
        .getOpenTelemetrySdk();
  }

  private OpenTelemetry setupNoopOpenTelemetry() {
    OpenTelemetry noop = OpenTelemetry.noop();
    GlobalOpenTelemetry.set(noop);
    return noop;
  }

  public static InitialBuilder builder() {
    return new Builder();
  }

  public interface InitialBuilder {
    /**
     * Specifies a custom telemetry instance to use. If this builder is used, this module will only
     * be a consumer of the given OpenTelemetry instance, and the provider must care about
     * registering this instance as global or not.
     *
     * @param openTelemetry The telemetry instance to use.
     * @return the same builder
     */
    FinalBuilder withOpenTelemetry(OpenTelemetry openTelemetry);

    /**
     * Enables the bundle to setup an auto configured instance of OpenTelemetry Sdk, and register it
     * as Global.
     *
     * @return the same builder.
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
    public FinalBuilder withOpenTelemetry(OpenTelemetry openTelemetry) {
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
      return new OpenTelemetryBundle(openTelemetryProvider, excludedUrlPatterns);
    }
  }

  private static boolean isOpenTelemetryDisabled() {
    // Skip loading the agent if tracing is disabled.
    String jaegerSamplerType = getProperty(JAEGER_SAMPLER_TYPE);
    String jaegerSamplerParam = getProperty(JAEGER_SAMPLER_PARAM);
    boolean isDisabledUsingLegacyVars =
        "const".equals(jaegerSamplerType) && "0".equals(jaegerSamplerParam);
    if (isDisabledUsingLegacyVars) {
      LOG.warn("Tracing is disabled using deprecated configuration.");
      return true;
    }
    return "true".equals(getProperty(TRACING_DISABLED));
  }

  private static String getProperty(String name) {
    return new SystemPropertyAndEnvironmentLookup().lookup(name);
  }
}
