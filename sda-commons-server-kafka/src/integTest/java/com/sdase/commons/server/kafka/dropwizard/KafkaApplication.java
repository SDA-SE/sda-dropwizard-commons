package com.sdase.commons.server.kafka.dropwizard;

import com.codahale.metrics.health.HealthCheck;
import com.sdase.commons.server.kafka.KafkaBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class KafkaApplication extends Application<AppConfiguration> {

   private KafkaBundle<AppConfiguration> kafkaBundle;

   @Override
   public void run(AppConfiguration configuration, Environment environment) {
      environment.healthChecks().register("dummy", new DummyHealthCheck());

   }

   @Override
   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      kafkaBundle = KafkaBundle.builder().withConfigurationProvider(AppConfiguration::getKafka).build();
      bootstrap.addBundle(kafkaBundle);
   }

   private static class DummyHealthCheck extends HealthCheck {

      @Override
      protected Result check() {
         return Result.healthy();
      }
   }

   public KafkaBundle<AppConfiguration> getKafkaBundle() {
      return kafkaBundle;
   }

}
