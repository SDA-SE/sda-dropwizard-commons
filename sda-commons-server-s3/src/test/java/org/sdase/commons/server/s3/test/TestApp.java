package org.sdase.commons.server.s3.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.s3.S3Bundle;

public class TestApp extends Application<Config> {

   private S3Bundle<Config> s3Bundle = S3Bundle.builder().withConfigurationProvider(Config::getS3Config).build();

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
}
