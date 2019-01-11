package org.sdase.commons.server.healthcheck.example.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example thread. It just counts every second and stops by a method call
 */
public class CountingThread extends Thread {

   private static final Logger LOG = LoggerFactory.getLogger(CountingThread.class);

   private int count = 0;
   private boolean stop = false;

   @Override
   public void run() {
      while (!stop) {
         try {
            Thread.sleep(1000);
            LOG.info("still counting {}", count++);

         } catch (InterruptedException e) {
            LOG.warn("Interrupted", e);
            Thread.currentThread().interrupt();
         }
      }
      LOG.info("stop counting");
   }

   public void setStop(boolean stop) {
      this.stop = stop;
   }

}
