package org.sdase.commons.server.cloudevents.app;

import io.dropwizard.Configuration;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.sdase.commons.server.kafka.KafkaConfiguration;

public class CloudEventsTestConfig extends Configuration {

  @NotNull @Valid private KafkaConfiguration kafka = new KafkaConfiguration();

  public CloudEventsTestConfig setKafka(KafkaConfiguration kafka) {
    this.kafka = kafka;
    return this;
  }

  public KafkaConfiguration getKafka() {
    return kafka;
  }
}
