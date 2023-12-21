package org.sdase.commons.server.kafka.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.kafka.test.KafkaBrokers;
import io.dropwizard.core.Configuration;
import java.util.List;
import org.sdase.commons.server.kafka.KafkaConfiguration;

public class KafkaTestConfiguration extends Configuration {

  @JsonProperty private KafkaConfiguration kafka = new KafkaConfiguration();

  public KafkaTestConfiguration() {}

  public KafkaTestConfiguration withBrokers(KafkaBrokers kafkaBrokers) {
    kafkaBrokers.forEach(b -> kafka.getBrokers().add(b.getConnectString()));
    return this;
  }

  public KafkaTestConfiguration withBrokers(List<String> kafkaBrokers) {
    kafka.getBrokers().addAll(kafkaBrokers);
    return this;
  }

  public KafkaConfiguration getKafka() {
    return kafka;
  }
}
