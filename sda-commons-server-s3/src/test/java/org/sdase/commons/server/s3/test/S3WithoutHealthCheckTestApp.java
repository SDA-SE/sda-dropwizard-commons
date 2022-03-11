package org.sdase.commons.server.s3.test;

import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.s3.S3Bundle;

public class S3WithoutHealthCheckTestApp extends Application<Config> {
  private S3Bundle<Config> s3Bundle =
      S3Bundle.builder().withConfigurationProvider(Config::getS3Config).build();

  private HealthCheckRegistry healthCheckRegistry;

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(s3Bundle);
  }

  @Override
  public void run(Config configuration, Environment environment) throws Exception {
    healthCheckRegistry = environment.healthChecks();
  }

  public HealthCheckRegistry healthCheckRegistry() {
    return healthCheckRegistry;
  }
}
