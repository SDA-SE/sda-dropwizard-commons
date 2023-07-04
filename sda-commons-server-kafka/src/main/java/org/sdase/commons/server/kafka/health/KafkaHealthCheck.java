package org.sdase.commons.server.kafka.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;
import org.slf4j.LoggerFactory;

@Async(period = 30, initialDelay = 10, scheduleType = Async.ScheduleType.FIXED_DELAY)
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
    int timeoutInMs = config.getHealthCheck().getTimeoutInSeconds() * 1000;
    try {
      adminClient.listTopics(new ListTopicsOptions().timeoutMs(timeoutInMs)).names().get();
    } catch (Exception e) {
      LOGGER.warn("Kafka health check failed", e);
      return Result.unhealthy("Connection to broker failed within " + timeoutInMs + "ms");
    }
    return Result.healthy();
  }

  public void shutdown() {
    adminClient.close();
  }
}
