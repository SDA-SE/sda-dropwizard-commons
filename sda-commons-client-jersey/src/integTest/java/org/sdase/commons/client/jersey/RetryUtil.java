package org.sdase.commons.client.jersey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RetryUtil {

   private static final Logger LOG = LoggerFactory.getLogger(RetryUtil.class);

   private RetryUtil() {
      // make sonar happy
   }

   static void tryNTimes(int n, Runnable r) {
      int retries = n + 1;
      for (int i = 0; i < retries + 1; i++) {
         try {
            r.run();
            break;
         } catch (Throwable t) { // NOSONAR
            if (i < retries) {
               LOG.warn("Attempt {} failed.", i + 1, t);
            } else {
               LOG.error("Finally failed after {} attempts.", i + 1);
               throw t;
            }
         }
      }
   }

}
