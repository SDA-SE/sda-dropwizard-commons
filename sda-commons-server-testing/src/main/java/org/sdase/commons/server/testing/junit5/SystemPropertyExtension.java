package org.sdase.commons.server.testing.junit5;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * This Junit 5 extension allows to set system properties for JUnit tests. All system properties
 * will be reset after the test has run.
 *
 * <p>Example:
 *
 * <pre>
 *   class MyTest {
 *
 *     &#64;RegisterExtension
 *     public SystemPropertyExtension PROP = new SystemPropertyExtension().setProperty("DISABLE_JWT", "true");
 *   }
 * </pre>
 */
public class SystemPropertyExtension implements BeforeAllCallback, AfterAllCallback {

  private final Map<String, Supplier<String>> propertiesToSet = new HashMap<>();

  private final Map<String, String> propertiesToReset = new HashMap<>();

  @Override
  public void beforeAll(ExtensionContext context) {
    propertiesToSet.forEach(
        (key, value) -> SystemPropertyExtension.this.applyProperty(key, value.get()));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    propertiesToReset.forEach(SystemPropertyExtension.this::applyProperty);
  }

  /**
   * Set a property to a computed value for the test. The {@code value} supplier is called when the
   * rule is started and the property is set. This is useful, if the {@link SystemPropertyExtension}
   * is part of a {@link org.junit.rules.RuleChain} and the value can only be accessed after other
   * rules have been started.
   *
   * @param key the property to set
   * @param value the supplier that provides the value to set
   * @return the rule
   */
  public SystemPropertyExtension setProperty(String key, Supplier<String> value) {
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
  public SystemPropertyExtension setProperty(String key, String value) {
    return setProperty(key, () -> value);
  }

  /**
   * Unset a property for the test.
   *
   * @param key the property to unser
   * @return the rule
   */
  public SystemPropertyExtension unsetProperty(String key) {
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
