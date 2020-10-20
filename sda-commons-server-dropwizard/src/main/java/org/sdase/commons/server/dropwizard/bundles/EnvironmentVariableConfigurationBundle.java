package org.sdase.commons.server.dropwizard.bundles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.setup.Bootstrap;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.Validator;

/**
 * This bundle extends the Dropwizard Support for overriding configurations via System Properties to
 * Environment variables.
 *
 * <p>Given the following simple configuration:
 *
 * <pre>
 * public class ExampleConfiguration extends Configuration {
 *     private String name;
 *
 *     private List&lt;String&gt; names = Collections.emptyList();
 *
 *     public String getName() {
 *         return name;
 *     }
 *
 *     public List&lt;String&gt; getNames() {
 *         return names;
 *     }
 * }
 * </pre>
 *
 * Override the entries with as follows:
 *
 * <pre>
 *   export dw.name="My Name"
 *   export dw.names="One,Two,Three"
 * </pre>
 *
 * Note that these kinds of environment variables might not be supported in any environment, but it
 * should be usable in Docker or Kubernetes environments.
 */
public class EnvironmentVariableConfigurationBundle implements ConfiguredBundle<Configuration> {
  public static EnvironmentVariableConfigurationBundle.Builder builder() {
    return new EnvironmentVariableConfigurationBundle.Builder();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    bootstrap.setConfigurationFactoryFactory(new EnvConfigurationFactoryFactory<>());
  }

  public static class Builder {
    public EnvironmentVariableConfigurationBundle build() {
      return new EnvironmentVariableConfigurationBundle();
    }
  }

  public static class EnvConfigurationFactoryFactory<T>
      extends DefaultConfigurationFactoryFactory<T> {
    @Override
    public ConfigurationFactory<T> create(
        Class<T> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
      return new EnvYamlConfigurationFactory<>(klass, validator, objectMapper, propertyPrefix);
    }
  }

  /**
   * A {@link YamlConfigurationFactory} that consumes all environment variables that begin with a
   * configurable {@code propertyPrefix} (Dropwizard defaults to {@code dw.}) and uses them as
   * overrides.
   *
   * @param <T> the type of the configuration objects to produce
   */
  public static class EnvYamlConfigurationFactory<T> extends YamlConfigurationFactory<T> {
    private final String propertyPrefix;

    /**
     * Creates a new configuration factory for the given class.
     *
     * @param klass the configuration class
     * @param validator the validator to use
     * @param objectMapper the Jackson {@link ObjectMapper} to use
     * @param propertyPrefix the system property name prefix used by overrides
     */
    public EnvYamlConfigurationFactory(
        Class<T> klass,
        @Nullable Validator validator,
        ObjectMapper objectMapper,
        String propertyPrefix) {
      super(klass, validator, objectMapper, propertyPrefix);

      this.propertyPrefix = propertyPrefix.endsWith(".") ? propertyPrefix : propertyPrefix + '.';
    }

    @Override
    protected T build(JsonNode node, String path) throws IOException, ConfigurationException {
      for (Map.Entry<String, String> pref : System.getenv().entrySet()) {
        final String prefName = pref.getKey();
        if (prefName.startsWith(propertyPrefix)) {
          final String configName = prefName.substring(propertyPrefix.length());
          addOverride(node, configName, System.getenv(prefName));
        }
      }

      return super.build(node, path);
    }
  }
}
