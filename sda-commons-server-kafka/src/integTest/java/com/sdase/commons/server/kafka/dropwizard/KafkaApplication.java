package com.sdase.commons.server.kafka.dropwizard;

import com.codahale.metrics.health.HealthCheck;
import com.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import com.sdase.commons.server.kafka.KafkaBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class KafkaApplication extends Application<AppConfiguration> {

   private KafkaBundle<AppConfiguration> kafkaBundle;

   @Override
   public void initialize(Bootstrap<AppConfiguration> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      kafkaBundle = KafkaBundle.builder().withConfigurationProvider(AppConfiguration::getKafka).build();
      bootstrap.addBundle(kafkaBundle);
   }

   @Override
   public void run(AppConfiguration configuration, Environment environment)  {

   }

   public KafkaBundle<AppConfiguration> getKafkaBundle() {
      return kafkaBundle;
   }

}
