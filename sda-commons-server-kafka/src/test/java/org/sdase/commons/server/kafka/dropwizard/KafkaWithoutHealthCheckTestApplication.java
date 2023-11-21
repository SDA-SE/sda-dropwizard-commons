package org.sdase.commons.server.kafka.dropwizard;

import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.KafkaBundle;

public class KafkaWithoutHealthCheckTestApplication extends Application<KafkaTestConfiguration> {

  private final KafkaBundle<KafkaTestConfiguration> bundle =
      KafkaBundle.builder()
          .withConfigurationProvider(KafkaTestConfiguration::getKafka)
          .withoutHealthCheck()
          .build();

  private HealthCheckRegistry healthCheckRegistry;

  @Override
  public void initialize(Bootstrap<KafkaTestConfiguration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(bundle);
  }

  @Override
  public void run(KafkaTestConfiguration configuration, Environment environment) {
    healthCheckRegistry = environment.healthChecks();
  }

  public HealthCheckRegistry healthCheckRegistry() {
    return healthCheckRegistry;
  }
}
