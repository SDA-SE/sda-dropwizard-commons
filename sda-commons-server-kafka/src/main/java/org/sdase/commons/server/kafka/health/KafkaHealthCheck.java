package org.sdase.commons.server.kafka.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;
import org.slf4j.LoggerFactory;

@Async(period = 30, scheduleType = Async.ScheduleType.FIXED_DELAY)
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
    try {
      adminClient
          .listTopics()
          .names()
          .get(config.getHealthCheck().getTimeoutInSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      LOGGER.warn("Kafka health check failed", e);
      return Result.unhealthy("Connection to broker failed within 2 seconds");
    }
    return Result.healthy();
  }

  public void shutdown() {
    adminClient.close();
  }
}
