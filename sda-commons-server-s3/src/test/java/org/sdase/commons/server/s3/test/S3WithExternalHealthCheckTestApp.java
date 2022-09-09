package org.sdase.commons.server.s3.test;

import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentelemetry.api.OpenTelemetry;
import java.util.Collections;
import org.sdase.commons.server.s3.S3Bundle;

public class S3WithExternalHealthCheckTestApp extends Application<Config> {
  public OpenTelemetry getOpenTelemetry() {
    return openTelemetry;
  }

  public S3WithExternalHealthCheckTestApp setOpenTelemetry(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    return this;
  }

  private OpenTelemetry openTelemetry;

  private HealthCheckRegistry healthCheckRegistry;

  private S3Bundle<Config> s3Bundle =
      S3Bundle.builder()
          .withConfigurationProvider(Config::getS3Config)
          .withExternalHealthCheck(Collections.singleton(Config::getS3Bucket))
          .withOpenTelemetry(openTelemetry)
          .build();

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(s3Bundle);
  }

  @Override
  public void run(Config configuration, Environment environment) {
    healthCheckRegistry = environment.healthChecks();
  }

  public HealthCheckRegistry healthCheckRegistry() {
    return healthCheckRegistry;
  }
}
