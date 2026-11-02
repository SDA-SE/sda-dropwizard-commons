package org.sdase.commons.server.kafka.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import org.sdase.commons.server.kafka.KafkaConfiguration;

public class KafkaTestConfiguration extends Configuration {

  @JsonProperty private KafkaConfiguration kafka = new KafkaConfiguration();

  public KafkaTestConfiguration() {}

  public KafkaConfiguration getKafka() {
    return kafka;
  }
}
