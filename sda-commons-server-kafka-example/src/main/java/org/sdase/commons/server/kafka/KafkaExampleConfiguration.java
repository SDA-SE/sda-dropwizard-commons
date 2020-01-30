package org.sdase.commons.server.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class KafkaExampleConfiguration extends Configuration {

  @JsonProperty private KafkaConfiguration kafka = new KafkaConfiguration();

  KafkaConfiguration getKafka() {
    return kafka;
  }
}
