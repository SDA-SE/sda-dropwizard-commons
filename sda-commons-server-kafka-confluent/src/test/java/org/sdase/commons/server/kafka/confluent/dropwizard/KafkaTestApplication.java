package org.sdase.commons.server.kafka.confluent.dropwizard;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.KafkaBundle;

public class KafkaTestApplication extends Application<KafkaTestConfiguration> {

  private KafkaBundle<KafkaTestConfiguration> bundle =
      KafkaBundle.builder().withConfigurationProvider(KafkaTestConfiguration::getKafka).build();

  @Override
  public void initialize(Bootstrap<KafkaTestConfiguration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(bundle);
  }

  @Override
  public void run(KafkaTestConfiguration configuration, Environment environment) {
    // empty
  }

  public KafkaBundle<KafkaTestConfiguration> kafkaBundle() {
    return bundle;
  }
}
