package org.sdase.commons.server.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;

public class KafkaExampleConfiguration extends Configuration {

  @JsonProperty private final KafkaConfiguration kafka = new KafkaConfiguration();

  KafkaConfiguration getKafka() {
    return kafka;
  }
}
