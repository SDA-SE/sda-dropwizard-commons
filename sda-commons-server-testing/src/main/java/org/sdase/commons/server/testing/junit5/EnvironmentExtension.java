package org.sdase.commons.server.testing.junit5;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.server.testing.Environment;

/**
 * This Junit 5 extension allows to set environment variables for JUnit tests. All environment
 * variables will be reset after the test has run.
 *
 * <p>Example:
 *
 * <pre>
 *   class MyTest {
 *
 *     &#64;RegisterExtension
 *     public final env = new EnvironmentExtension().setEnv("DISABLE_JWT", "true");
 *
 *   }
 * </pre>
 */
public class EnvironmentExtension implements BeforeAllCallback, AfterAllCallback {

  private final Map<String, Supplier<String>> envToSet = new HashMap<>();

  private final Map<String, String> envToReset = new HashMap<>();

  @Override
  public void beforeAll(ExtensionContext context) {
    envToSet.forEach((key, value) -> applyEnv(key, value.get()));
  }

  @Override
  public void afterAll(ExtensionContext context) {
    envToReset.forEach(EnvironmentExtension.this::applyEnv);
  }

  public EnvironmentExtension setEnv(String key, String value) {
    return setEnv(key, () -> value);
  }

  /**
   * Set an environment variable to a computed value for the test. The {@code value} supplier is
   * called when the rule is started and the property is set. This is useful, if the {@link
   * EnvironmentExtension} is part of a {@link org.junit.rules.RuleChain} and the value can only be
   * accessed after other rules have been started.
   *
   * @param key the property to set
   * @param value the supplier that provides the value to set
   * @return the rule
   */
  public EnvironmentExtension setEnv(String key, Supplier<String> value) {
    envToSet.put(key, value);
    envToReset.put(key, System.getenv(key));
    return this;
  }

  public EnvironmentExtension unsetEnv(String key) {
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
