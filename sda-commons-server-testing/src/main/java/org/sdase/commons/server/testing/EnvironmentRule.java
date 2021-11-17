package org.sdase.commons.server.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
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
 *
 * @deprecated changing environment variables is not easily supported in Java 17; please consider
 *     using {@link SystemPropertyRule} instead
 */
@Deprecated
public class EnvironmentRule implements TestRule {

  private Map<String, Supplier<String>> envToSet = new HashMap<>();

  private Map<String, String> envToReset = new HashMap<>();

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        envToSet.forEach((key, value) -> applyEnv(key, value.get()));
        try {
          base.evaluate();
        } finally {
          envToReset.forEach(EnvironmentRule.this::applyEnv);
        }
      }
    };
  }

  public EnvironmentRule setEnv(String key, String value) {
    return setEnv(key, () -> value);
  }

  /**
   * Set an environment variable to a computed value for the test. The {@code value} supplier is
   * called when the rule is started and the property is set. This is useful, if the {@link
   * EnvironmentRule} is part of a {@link org.junit.rules.RuleChain} and the value can only be
   * accessed after other rules have been started.
   *
   * @param key the property to set
   * @param value the supplier that provides the value to set
   * @return the rule
   */
  public EnvironmentRule setEnv(String key, Supplier<String> value) {
    envToSet.put(key, value);
    envToReset.put(key, System.getenv(key));
    return this;
  }

  public EnvironmentRule unsetEnv(String key) {
    envToSet.put(key, () -> null);
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
