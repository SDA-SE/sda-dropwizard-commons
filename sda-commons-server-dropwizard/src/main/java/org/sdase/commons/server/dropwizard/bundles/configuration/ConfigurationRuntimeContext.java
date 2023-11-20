package org.sdase.commons.server.dropwizard.bundles.configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A context of configured properties at runtime. A context may be implemented as an aggregate with
 * defined precedence of multiple contexts.
 */
public interface ConfigurationRuntimeContext {

  /**
   * Instance of a {@link ConfigurationRuntimeContext} that is backed by {@linkplain System#getenv()
   * environment variables}.
   */
  ConfigurationRuntimeContext FROM_ENVIRONMENT = new EnvironmentConfigurationRuntimeContext();

  /**
   * Instance of a {@link ConfigurationRuntimeContext} that is backed by {@linkplain
   * System#getProperties() system properties}.
   */
  ConfigurationRuntimeContext FROM_SYSTEM_PROPERTIES =
      new SystemPropertiesConfigurationRuntimeContext();

  /**
   * Instance of a {@link ConfigurationRuntimeContext} that is backed with higher precedence by
   * {@linkplain System#getProperties() system properties} and with lower precedence by {@linkplain
   * System#getenv() environment variables}.
   */
  ConfigurationRuntimeContext FROM_SYSTEM_PROPERTIES_AND_ENVIRONMENT =
      new AggregateConfigurationRuntimeContext(
          ConfigurationRuntimeContext.FROM_SYSTEM_PROPERTIES,
          ConfigurationRuntimeContext.FROM_ENVIRONMENT);

  /**
   * Determines all keys that are defined in the current context.
   *
   * @return all defined keys in the context
   */
  Set<String> getDefinedKeys();

  /**
   * Determines the value with the highest precedence for the given {@code key} in the current
   * context.
   *
   * @param key the key to lookup
   * @return the value or {@code null} if not defined in the current context.
   */
  String getValue(String key);

  /**
   * A {@link ConfigurationRuntimeContext} that checks multiple {@link
   * ConfigurationRuntimeContext}'s in a defined order. Data of considered contexts is not cached.
   * Changes in the contexts at runtime will propagate to the aggregate.
   */
  class AggregateConfigurationRuntimeContext implements ConfigurationRuntimeContext {

    private final List<ConfigurationRuntimeContext> delegateContexts;

    /**
     * @param delegateContexts The {@link ConfigurationRuntimeContext}s that are considered in the
     *     given order. {@link #getValue(String)} returns value of the first context that does not
     *     return {@code null} for a given {@code key}. A null-value that is defined for specific
     *     key is ignored.
     */
    public AggregateConfigurationRuntimeContext(ConfigurationRuntimeContext... delegateContexts) {
      this.delegateContexts = List.of(delegateContexts);
    }

    @Override
    public Set<String> getDefinedKeys() {
      Set<String> keys = new LinkedHashSet<>();
      for (ConfigurationRuntimeContext delegateContext : delegateContexts) {
        keys.addAll(delegateContext.getDefinedKeys());
      }

      return keys;
    }

    @Override
    public String getValue(String key) {
      for (ConfigurationRuntimeContext delegateContext : delegateContexts) {
        var value = delegateContext.getValue(key);
        if (value != null) {
          return value;
        }
      }
      return null;
    }
  }

  /**
   * A {@link ConfigurationRuntimeContext} that is backed by {@linkplain System#getenv() environment
   * variables}.
   */
  class EnvironmentConfigurationRuntimeContext implements ConfigurationRuntimeContext {

    @Override
    public Set<String> getDefinedKeys() {
      return System.getenv().keySet();
    }

    @Override
    public String getValue(String key) {
      return System.getenv(key);
    }
  }

  /**
   * A {@link ConfigurationRuntimeContext} that is backed by {@linkplain System#getProperties()
   * system properties}.
   */
  class SystemPropertiesConfigurationRuntimeContext implements ConfigurationRuntimeContext {

    @Override
    public Set<String> getDefinedKeys() {
      return System.getProperties().keySet().stream()
          .map(Object::toString)
          .collect(Collectors.toSet());
    }

    @Override
    public String getValue(String key) {
      return System.getProperty(key);
    }
  }
}
