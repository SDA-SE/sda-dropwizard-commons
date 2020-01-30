package org.sdase.commons.server.weld.testing.test.util;

import static org.slf4j.LoggerFactory.getLogger;

import javax.enterprise.event.Observes;
import org.slf4j.Logger;

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
