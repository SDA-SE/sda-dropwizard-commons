package org.sdase.commons.server.s3.test;

import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.mock.MockTracer;
import org.sdase.commons.server.s3.S3Bundle;

public class S3WithExternalHealthCheckTestApp extends Application<Config> {

  private final MockTracer mockTracer = new MockTracer();

  private HealthCheckRegistry healthCheckRegistry;

  private S3Bundle<Config> s3Bundle =
      S3Bundle.builder()
          .withConfigurationProvider(Config::getS3Config)
          .withTracer(mockTracer)
          .withExternalHealthCheck("testbucket")
          .build();

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(s3Bundle);
  }

  @Override
  public void run(Config configuration, Environment environment) {
    healthCheckRegistry = environment.healthChecks();
  }

  public S3Bundle<Config> getS3Bundle() {
    return s3Bundle;
  }

  public MockTracer getMockTracer() {
    return mockTracer;
  }

  public HealthCheckRegistry healthCheckRegistry() {
    return healthCheckRegistry;
  }
}
