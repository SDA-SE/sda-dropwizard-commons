package org.sdase.commons.server.s3.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.mock.MockTracer;
import org.sdase.commons.server.s3.S3Bundle;

public class TestApp extends Application<Config> {

  private final MockTracer mockTracer = new MockTracer();

  private S3Bundle<Config> s3Bundle =
      S3Bundle.builder()
          .withConfigurationProvider(Config::getS3Config)
          .withTracer(mockTracer)
          .build();

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(s3Bundle);
  }

  @Override
  public void run(Config configuration, Environment environment) {
    // nothing to run
  }

  public S3Bundle<Config> getS3Bundle() {
    return s3Bundle;
  }

  public MockTracer getMockTracer() {
    return mockTracer;
  }
}
