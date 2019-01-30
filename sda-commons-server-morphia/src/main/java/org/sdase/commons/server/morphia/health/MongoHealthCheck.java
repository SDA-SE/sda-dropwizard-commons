package org.sdase.commons.server.morphia.health;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoHealthCheck  extends HealthCheck {

   private static final Logger LOGGER = LoggerFactory.getLogger(MongoHealthCheck.class);
   private static final Document PING = new Document("ping", 1);

   private final MongoDatabase db;

   public MongoHealthCheck(MongoDatabase db) {
      this.db = db;
   }

   @Override
   protected Result check() {

      try {
         Document result = db.runCommand(PING);
         int ok = 0;

         if (result.containsKey("ok") && result.get("ok") instanceof Number) {
            ok = result.get("ok", Number.class).intValue();
         }

         if (ok != 1) {
            LOGGER.warn("Unexpected ping response: {}", result);
            return Result.unhealthy("Unexpected ping response: " + result.toString());
         }

         return Result.healthy();

      } catch (Exception e) {
         LOGGER.warn("Failed health check", e);
         return Result.unhealthy(e.getMessage());
      }
   }
}
