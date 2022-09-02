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
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.sdase.commons.server.opentelemetry.autoconfig.SdaConfigPropertyProvider;
import org.sdase.commons.server.opentelemetry.jaxrs.JerseyExceptionListener;
import org.sdase.commons.server.opentelemetry.jaxrs.ServerTracingFilter;
import org.sdase.commons.server.opentelemetry.servlet.TracingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenTelemetryBundle implements ConfiguredBundle<Configuration> {
  private static final Logger LOG = LoggerFactory.getLogger(OpenTelemetryBundle.class);
  private static final String MAIN_THREAD_CHECK_ENABLED = "MAIN_THREAD_CHECK_ENABLED";
  private final OpenTelemetry openTelemetry;
  private final Pattern excludedUrlPatterns;

  public OpenTelemetryBundle(OpenTelemetry openTelemetry, Pattern excludedUrlPatterns) {
    this.openTelemetry = openTelemetry;
    this.excludedUrlPatterns = excludedUrlPatterns;
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    registerJaxrsTracer(environment.jersey());
    registerServletTracer(environment, this.openTelemetry);
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
    LOG.info("No OpenTelemetry sdk instance is provided, using autoConfigured instance.");
    return AutoConfiguredOpenTelemetrySdk.builder()
        .addPropertiesSupplier(SdaConfigPropertyProvider::getProperties)
        .setResultAsGlobal(shouldSetAsGlobal())
        .build()
        .getOpenTelemetrySdk();
  }

  public static InitialBuilder builder() {
    return new Builder();
  }

  public interface InitialBuilder {
    /**
     * Specifies a custom telemetry instance to use. If this builder is used, this module will only
     * be a consumer, and the provider needs to take care of registering this instance as global.
     *
     * @param openTelemetry The telemetry instance to use.
     * @return the same builder
     */
    FinalBuilder withTelemetryInstance(OpenTelemetry openTelemetry);

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

  /**
   * Decides if the {@link OpenTelemetry} instance created by {@link AutoConfiguredOpenTelemetrySdk}
   * should be registered as the global instance. It can be troublesome for consumers of the module
   * and will break many tests, as they have to explicitly unregister this for tests using {@link
   * GlobalOpenTelemetry#resetForTest()} in their tests.
   *
   * @return the decision
   */
  private static boolean shouldSetAsGlobal() {
    // Skip loading the agent if not triggered from the main thread.
    boolean isMainThreadCheckDisabled = "false".equals(getProperty(MAIN_THREAD_CHECK_ENABLED));
    boolean isMainThread = "main".equals(Thread.currentThread().getName());
    if (!isMainThreadCheckDisabled && !isMainThread) {
      LOG.warn("Skipping setting the TelemetrySdk as global.");
      return false;
    }
    return true;
  }

  private static String getProperty(String name) {
    return new SystemPropertyAndEnvironmentLookup().lookup(name);
  }
}
