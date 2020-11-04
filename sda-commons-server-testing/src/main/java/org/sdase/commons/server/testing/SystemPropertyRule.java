package org.sdase.commons.server.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
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

  private final Map<String, Supplier<String>> propertiesToSet = new HashMap<>();

  private final Map<String, String> propertiesToReset = new HashMap<>();

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        propertiesToSet.forEach(
            (key, value) -> SystemPropertyRule.this.applyProperty(key, value.get()));
        try {
          base.evaluate();
        } finally {
          propertiesToReset.forEach(SystemPropertyRule.this::applyProperty);
        }
      }
    };
  }

  /**
   * Set a property to a computed value for the test. The {@code value} supplier is called when the
   * rule is started and the property is set. This is useful, if the {@link SystemPropertyRule} is
   * part of a {@link org.junit.rules.RuleChain} and the value can only be access after other rules
   * have started.
   *
   * @param key the property to set
   * @param value the supplier that provides the value to set
   * @return the rule
   */
  public SystemPropertyRule setProperty(String key, Supplier<String> value) {
    propertiesToSet.put(key, value);
    propertiesToReset.put(key, System.getProperty(key));
    return this;
  }

  /**
   * Set a property to a value for the test..
   *
   * @param key the property to set
   * @param value the value to set
   * @return the rule
   */
  public SystemPropertyRule setProperty(String key, String value) {
    return setProperty(key, () -> value);
  }

  /**
   * Unset a property for the test.
   *
   * @param key the property to unser
   * @return the rule
   */
  public SystemPropertyRule unsetProperty(String key) {
    propertiesToSet.put(key, () -> null);
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
