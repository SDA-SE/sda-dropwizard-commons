package org.sdase.commons.server.testing;

import java.util.HashMap;
import java.util.Map;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This {@link TestRule} allows to set environment variables for JUnit tests. All environment
 * variables will be reset after the test has run.
 *
 * <p>Example:
 *
 * <pre>
 *   class MyTest {
 *     &#64;ClassRule public static final ENV = new EnvironmentRule().setEnv("DISABLE_JWT", "true");
 *   }
 * </pre>
 */
public class EnvironmentRule implements TestRule {

  private Map<String, String> envToSet = new HashMap<>();

  private Map<String, String> envToReset = new HashMap<>();

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        envToSet.forEach(EnvironmentRule.this::applyEnv);
        try {
          base.evaluate();
        } finally {
          envToReset.forEach(EnvironmentRule.this::applyEnv);
        }
      }
    };
  }

  public EnvironmentRule setEnv(String key, String value) {
    envToSet.put(key, value);
    envToReset.put(key, System.getenv(key));
    return this;
  }

  public EnvironmentRule unsetEnv(String key) {
    envToSet.put(key, null);
    envToReset.put(key, System.getenv(key));
    return this;
  }

  private void applyEnv(String key, String value) {
    if (value == null) {
      Environment.unsetEnv(key);
    } else {
      Environment.setEnv(key, value);
    }
  }
}
