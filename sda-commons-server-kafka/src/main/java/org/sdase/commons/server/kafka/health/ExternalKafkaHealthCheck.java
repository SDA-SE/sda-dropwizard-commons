package org.sdase.commons.server.kafka.health;

import org.sdase.commons.server.dropwizard.healthcheck.ExternalHealthCheck;
import org.sdase.commons.server.kafka.KafkaConfiguration;

@ExternalHealthCheck
public class ExternalKafkaHealthCheck extends KafkaHealthCheck {

  public ExternalKafkaHealthCheck(KafkaConfiguration config) {
    super(config);
  }
}
