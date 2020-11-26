package org.sdase.commons.server.kms.aws.kafka.dropwizard;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kms.aws.kafka.KmsAwsKafkaBundle;

public class AwsEncryptionTestApplication extends Application<KmsAwsKafkaTestConfiguration> {

  private final KmsAwsKafkaBundle<KmsAwsKafkaTestConfiguration> kmsAwsKafkaBundle =
      KmsAwsKafkaBundle.builder()
          .withConfigurationProvider(KmsAwsKafkaTestConfiguration::getKmsAws)
          .build();

  private final KafkaBundle<KmsAwsKafkaTestConfiguration> kafkaBundle =
      KafkaBundle.builder()
          .withConfigurationProvider(KmsAwsKafkaTestConfiguration::getKafka)
          .build();

  @Override
  public void initialize(Bootstrap<KmsAwsKafkaTestConfiguration> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(kafkaBundle);
    bootstrap.addBundle(kmsAwsKafkaBundle);
  }

  @Override
  public void run(KmsAwsKafkaTestConfiguration configuration, Environment environment) {}

  public KmsAwsKafkaBundle<KmsAwsKafkaTestConfiguration> getAwsEncryptionBundle() {
    return kmsAwsKafkaBundle;
  }

  public KafkaBundle<KmsAwsKafkaTestConfiguration> kafkaBundle() {
    return kafkaBundle;
  }
}
