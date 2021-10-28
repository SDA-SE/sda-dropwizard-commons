package org.sdase.commons.server.jaeger;

import static org.sdase.commons.server.dropwizard.lifecycle.ManagedShutdownListener.onShutdown;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.CodecConfiguration;
import io.jaegertracing.Configuration.Propagation;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import org.sdase.commons.server.jaeger.metrics.PrometheusMetricsFactory;

/**
 * Configures the Jaeger client to sample to Jaeger. Configuration is done via environment
 * variables.
 *
 * @see <a href="https://www.jaegertracing.io/docs/1.16/client-features/">Jaeger Configuration</a>
 */
public class JaegerBundle implements ConfiguredBundle<io.dropwizard.Configuration> {

  private static final String JAEGER_SERVICE_NAME = "JAEGER_SERVICE_NAME";

  private JaegerBundle() {}

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // Nothing to initialize
  }

  @Override
  public void run(io.dropwizard.Configuration configuration, Environment environment) {
    SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv();

    if (samplerConfig.getType() == null && samplerConfig.getParam() == null) {
      // If nothing else is configured, sample everything
      samplerConfig.withType("const").withParam(1);
    }

    ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv();
    CodecConfiguration codecConfig =
        CodecConfiguration.fromEnv()
            .withPropagation(Propagation.B3)
            .withPropagation(Propagation.JAEGER);

    String serviceName = getProperty(JAEGER_SERVICE_NAME);

    if (serviceName == null) {
      // If no service name is provided via env, use the applications name
      serviceName = environment.getName();
    }

    Configuration config =
        io.jaegertracing.Configuration.fromEnv(serviceName)
            .withSampler(samplerConfig)
            .withCodec(codecConfig)
            .withReporter(reporterConfig);

    PrometheusMetricsFactory prometheusMetricsFactory = new PrometheusMetricsFactory();
    Tracer tracer = config.getTracerBuilder().withMetricsFactory(prometheusMetricsFactory).build();

    if (!GlobalTracer.registerIfAbsent(tracer)) {
      throw new IllegalStateException(
          "Couldn't register Jaeger tracer. There is already a global tracer!");
    }

    // Reset global tracer once the applications is stopped. This is important
    // to be able to start a new service instance in the same JVM because the
    // tracer is a global that normally can only be set once.
    //
    // It's not perfect that GlobalTracerTestUtil comes from a test-jar, but
    // we could also copy the code into this project.
    environment.lifecycle().manage(onShutdown(GlobalTracerTestUtil::resetGlobalTracer));
    environment.lifecycle().manage(onShutdown(tracer::close));
    environment.lifecycle().manage(prometheusMetricsFactory);
  }

  public static class Builder {

    private Builder() {}

    public JaegerBundle build() {
      return new JaegerBundle();
    }
  }

  private static String getProperty(String name) {
    return System.getProperty(name, System.getenv(name));
  }
}
