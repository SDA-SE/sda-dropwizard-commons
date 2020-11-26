package org.sdase.commons.server.kms.aws.dropwizard;

import com.codahale.metrics.health.HealthCheckRegistry;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kms.aws.KmsAwsBundle;

public class AwsEncryptionTestApplication extends Application<AwsEncryptionTestConfiguration> {

  private final KmsAwsBundle<AwsEncryptionTestConfiguration> bundle =
      KmsAwsBundle.builder()
          .withConfigurationProvider(AwsEncryptionTestConfiguration::getKmsAws)
          .build();

  private HealthCheckRegistry healthCheckRegistry;

  @Override
  public void initialize(Bootstrap<AwsEncryptionTestConfiguration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(bundle);
  }

  @Override
  public void run(AwsEncryptionTestConfiguration configuration, Environment environment) {
    healthCheckRegistry = environment.healthChecks();
  }

  public KmsAwsBundle<AwsEncryptionTestConfiguration> AwsEncryptionBundle() {
    return bundle;
  }

  public HealthCheckRegistry healthCheckRegistry() {
    return healthCheckRegistry;
  }
}
