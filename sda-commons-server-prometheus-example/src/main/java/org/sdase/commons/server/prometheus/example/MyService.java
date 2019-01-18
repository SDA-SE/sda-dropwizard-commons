package org.sdase.commons.server.prometheus.example;

import io.prometheus.client.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * A service that produces metrics about important things.
 */
public class MyService {

   private static final Logger LOG = LoggerFactory.getLogger(MyService.class);

   private Histogram someOperationDurationTracker = Histogram
         .build("some_operation_execution_duration_seconds", "Tracks duration of some operation.")
         .register();

   private Random random = new Random();

   public void doSomeOperation() {

      someOperationDurationTracker.time(() -> {
         // here some business logic is invoked which execution time will be tracked
         try {
            Thread.sleep(random.nextInt(2000) + 500);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      });

   }

}
