package org.sdase.commons.server.kafka.health;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.annotation.Async;
import org.apache.kafka.clients.admin.AdminClient;

import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.KafkaProperties;

import java.util.concurrent.TimeUnit;


@Async(period = 30, scheduleType = Async.ScheduleType.FIXED_DELAY)
public class KafkaHealthCheck extends HealthCheck {


   private final KafkaConfiguration config;

   public KafkaHealthCheck(KafkaConfiguration config) {
      this.config = config;
   }

   @Override
   protected Result check() {
      try (AdminClient adminClient = AdminClient.create(KafkaProperties.forAdminClient(config))){
         adminClient.listTopics().names().get(2, TimeUnit.SECONDS);
      } catch (Exception e) {
         return Result.unhealthy("Connection to broker failed within 2 seconds");
      }
      return Result.healthy();
   }

}
