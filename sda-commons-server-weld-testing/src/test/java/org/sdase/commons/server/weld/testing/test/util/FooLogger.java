package org.sdase.commons.server.weld.testing.test.util;

import org.slf4j.Logger;

import javax.enterprise.event.Observes;

import static org.slf4j.LoggerFactory.getLogger;

public class FooLogger {

   public static class Foo {
      public String hello() {
         return "================ i am in foo!";
      }
   }

   private final Logger logger = getLogger(this.getClass());

   public void logFoo(@Observes final Foo foo) {
      logger.error(foo.hello());
   }
}
