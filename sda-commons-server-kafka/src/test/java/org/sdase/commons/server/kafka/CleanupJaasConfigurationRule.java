package org.sdase.commons.server.kafka;

import javax.security.auth.login.Configuration;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A {@link TestRule} that resets the {@link javax.security.auth.login.Configuration} after
 * executing the tests. This might be required if different tests use different values in the {@code
 * java.security.auth.login.config} System property in the same JVM.
 */
public class CleanupJaasConfigurationRule implements TestRule {
  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        // reset the configuration state
        Configuration.setConfiguration(null);

        // run the nested rules and tests
        base.evaluate();
      }
    };
  }
}
