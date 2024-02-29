package org.sdase.commons.server.kafka.health;

import com.codahale.metrics.health.HealthCheck;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;
import org.slf4j.LoggerFactory;

public class KafkaHealthCheck extends HealthCheck {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(KafkaHealthCheck.class);

  private final KafkaConfiguration config;

  private final AdminClient adminClient;

  public KafkaHealthCheck(KafkaConfiguration config) {
    this.config = config;
    this.adminClient = AdminClient.create(KafkaProperties.forAdminClient(config));
  }

  @Override
  protected Result check() {
    int timeoutInSeconds = config.getHealthCheck().getTimeoutInSeconds();
    try {
      adminClient.listTopics().names().get(timeoutInSeconds, TimeUnit.SECONDS);
    } catch (Exception e) {
      LOGGER.warn("Kafka health check failed", e);
      return Result.unhealthy(
          "Connection to broker failed within " + timeoutInSeconds + " seconds");
    }
    return Result.healthy();
  }

  public void shutdown() {
    adminClient.close();
  }
}
