package org.sdase.commons.server.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension for setting system properties for (integration) tests.
 *
 * <p>Example usage:
 *
 * <pre>
 *   &#64;RegisterExtension
 *   static final SystemPropertyClassExtension PROP =
 *       new SystemPropertyClassExtension()
 *           .setProperty("name", "value");
 * </pre>
 */
public class SystemPropertyClassExtension implements BeforeAllCallback, AfterAllCallback {

  final Map<String, Supplier<String>> propertiesToSet = new HashMap<>();

  final Map<String, String> propertiesToReset = new HashMap<>();

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    propertiesToSet.forEach(
        (key, value) -> {
          propertiesToReset.put(key, System.getProperty(key));
          this.applyProperty(key, value.get());
        });
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    propertiesToReset.forEach(this::applyProperty);
  }

  /**
   * Set a property to a computed value for the test. The {@code value} supplier is called when the
   * extension is started and the property is set. This is useful, if the {@link
   * SystemPropertyClassExtension} is called in a specific order and the value can only be accessed
   * after other extensions have been started.
   *
   * @param key the property to set
   * @param value the supplier that provides the value to set
   * @return the rule
   */
  public SystemPropertyClassExtension setProperty(String key, Supplier<String> value) {
    propertiesToSet.put(key, value);
    return this;
  }

  /**
   * Set a property to a value for the test.
   *
   * @param key the property to set
   * @param value the value to set
   * @return the rule
   */
  public SystemPropertyClassExtension setProperty(String key, String value) {
    return setProperty(key, () -> value);
  }

  /**
   * Unset a property for the test.
   *
   * @param key the property to unser
   * @return the rule
   */
  public SystemPropertyClassExtension unsetProperty(String key) {
    propertiesToSet.put(key, () -> null);
    return this;
  }

  void applyProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
