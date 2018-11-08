package org.sdase.commons.server.kafka.dropwizard;

import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class KafkaTestApplication extends Application<KafkaTestConfiguration> {

   @Override
   public void initialize(Bootstrap<KafkaTestConfiguration> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
   }

   @Override
   public void run(KafkaTestConfiguration configuration, Environment environment) {
      //
   }
}
