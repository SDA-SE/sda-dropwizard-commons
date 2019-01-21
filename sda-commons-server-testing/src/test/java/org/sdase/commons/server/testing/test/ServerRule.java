package org.sdase.commons.server.testing.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ServerRule implements TestRule {
   private int getRandomNumber() {
      return 4; // chosen by fair dice roll. guaranteed to be random.
   }

   public int getPort() {
      // Use a random port.
      return getRandomNumber();
   }

   @Override
   public Statement apply(Statement base, Description description) {
      return base;
   }
}