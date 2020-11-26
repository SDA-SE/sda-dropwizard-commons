package org.sdase.commons.server.kms.aws.kafka.dropwizard;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kms.aws.config.KmsAwsConfiguration;

public class KmsAwsKafkaTestConfiguration extends Configuration {

  public KmsAwsKafkaTestConfiguration() {}

  @Valid private KafkaConfiguration kafka = new KafkaConfiguration();

  @Valid private KmsAwsConfiguration kmsAws = new KmsAwsConfiguration();

  public KmsAwsConfiguration getKmsAws() {
    return kmsAws;
  }

  public KafkaConfiguration getKafka() {
    return kafka;
  }

  public KmsAwsKafkaTestConfiguration setKafka(KafkaConfiguration kafka) {
    this.kafka = kafka;
    return this;
  }

  public KmsAwsKafkaTestConfiguration setKmsAws(KmsAwsConfiguration kmsAws) {
    this.kmsAws = kmsAws;
    return this;
  }
}
