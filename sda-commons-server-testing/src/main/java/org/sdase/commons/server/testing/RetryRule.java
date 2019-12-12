package org.sdase.commons.server.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retry flaky tests. Used together with the {@link Retry} annotation.
 */
public class RetryRule implements TestRule {

   private static class RetryStatement extends Statement {
      private static final Logger LOGGER = LoggerFactory.getLogger(RetryRule.class);

      private final Statement statement;
      private final int repeat;

      public RetryStatement(Statement statement, int repeat) {
         this.statement = statement;
         this.repeat = repeat;
      }

      @Override
      public void evaluate() throws Throwable {
         for (int i = 0; i < repeat; i++) {
            try {
               statement.evaluate();
               return;
            } catch (Throwable throwable) {
               if (i < repeat - 1) {
                  LOGGER.warn("Test failed, but repeating test to see if it fails again ({}/{})", i, repeat, throwable);
               } else {
                  LOGGER.warn("Test failed {} times, giving up", repeat, throwable);
                  throw throwable;
               }
            }
         }
      }
   }

   @Override
   public Statement apply(Statement statement, Description description) {
      Statement result = statement;

      Retry retry = description.getAnnotation(Retry.class);
      if (retry != null) {
         int times = retry.value();
         result = new RetryStatement(statement, times);
      }
      return result;
   }
}
