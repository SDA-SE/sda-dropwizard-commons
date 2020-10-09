package org.sdase.commons.server.kafka.health;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;
import org.slf4j.LoggerFactory;

@Async(period = 30, scheduleType = Async.ScheduleType.FIXED_DELAY)
public class KafkaHealthCheck extends HealthCheck {

  private final KafkaConfiguration config;

  public KafkaHealthCheck(KafkaConfiguration config) {
    this.config = config;
  }

  @Override
  protected Result check() {
    // Set logger for config to WARN, as it would otherwise print the full
    // config on every health check to the log.
    Logger logger = (Logger) LoggerFactory.getLogger(AdminClientConfig.class);
    Level oldLevel = logger.getLevel();
    logger.setLevel(Level.WARN);

    try (AdminClient adminClient = AdminClient.create(KafkaProperties.forAdminClient(config))) {
      adminClient
          .listTopics()
          .names()
          .get(config.getHealthCheck().getTimeoutInSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      return Result.unhealthy("Connection to broker failed within 2 seconds");
    } finally {
      logger.setLevel(oldLevel);
    }
    return Result.healthy();
  }
}
