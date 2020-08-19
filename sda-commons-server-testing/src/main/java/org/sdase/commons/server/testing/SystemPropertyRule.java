package org.sdase.commons.server.testing;

import java.util.HashMap;
import java.util.Map;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This {@link TestRule} allows to set system properties for JUnit tests. All system properties will
 * be reset after the test has run.
 *
 * <p>Example:
 *
 * <pre>
 *   class MyTest {
 *     &#64;ClassRule public static final SystemPropertyRule PROP = new SystemPropertyRule().setProperty("DISABLE_JWT", "true");
 *   }
 * </pre>
 */
public class SystemPropertyRule implements TestRule {

  private final Map<String, String> propertiesToSet = new HashMap<>();

  private final Map<String, String> propertiesToReset = new HashMap<>();

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        propertiesToSet.forEach(SystemPropertyRule.this::applyProperty);
        try {
          base.evaluate();
        } finally {
          propertiesToReset.forEach(SystemPropertyRule.this::applyProperty);
        }
      }
    };
  }

  public SystemPropertyRule setProperty(String key, String value) {
    propertiesToSet.put(key, value);
    propertiesToReset.put(key, System.getProperty(key));
    return this;
  }

  public SystemPropertyRule unsetProperty(String key) {
    propertiesToSet.put(key, null);
    propertiesToReset.put(key, System.getProperty(key));
    return this;
  }

  private void applyProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
