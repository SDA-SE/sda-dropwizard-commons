package org.sdase.commons.server.dropwizard.bundles.configuration.generic;

import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.DROPWIZARD_PLAIN_TYPES;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.Validator;
import org.apache.commons.text.lookup.StringLookup;
import org.sdase.commons.server.dropwizard.bundles.configuration.ConfigurationRuntimeContext;
import org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner;
import org.sdase.commons.server.dropwizard.bundles.scanner.MappableField;

/**
 * A {@link YamlConfigurationFactory} that consumes all environment variables that match known
 * properties in the configuration class. Properties are converted to environment variable keys by
 * reflecting the hierarchy with an underscore as separate. All properties are converted from
 * camelCase to UPPERCASE.
 *
 * @param <T> the type of the configuration objects to produce
 * @see JacksonTypeScanner
 */
public class GenericLookupYamlConfigurationFactory<T> extends YamlConfigurationFactory<T> {

  private final Class<T> configurationClass;
  private final JacksonTypeScanner jacksonTypeScanner;
  private final ConfigurationRuntimeContext configurationRuntimeContext;

  /**
   * Creates a new configuration factory for the given class.
   *
   * @param configurationClass the configuration class
   * @param validator the validator to use
   * @param objectMapper the Jackson {@link ObjectMapper} to use
   * @param propertyPrefix the system property name prefix used by overrides
   * @param jacksonTypeScanner the scanner to derive available properties
   * @param configurationRuntimeContext the {@link StringLookup} to lookup values for {@link
   *     MappableField#getContextKey()}
   */
  public GenericLookupYamlConfigurationFactory(
      Class<T> configurationClass,
      @Nullable Validator validator,
      ObjectMapper objectMapper,
      String propertyPrefix,
      JacksonTypeScanner jacksonTypeScanner,
      ConfigurationRuntimeContext configurationRuntimeContext) {
    super(configurationClass, validator, objectMapper, propertyPrefix);
    this.configurationClass = configurationClass;
    this.jacksonTypeScanner = jacksonTypeScanner;
    this.configurationRuntimeContext = configurationRuntimeContext;
  }

  @Override
  protected T build(JsonNode node, String path) throws IOException, ConfigurationException {
    List<MappableField> mappableFields = jacksonTypeScanner.scan(configurationClass);
    mappableFields.stream()
        .map(m -> m.expand(configurationRuntimeContext.getDefinedKeys()))
        .flatMap(List::stream)
        .forEach(mappableField -> configure(node, mappableField));
    return super.build(node, path);
  }

  private void configure(JsonNode baseNode, MappableField mappableField) {
    String jsonPathOfField = String.join(".", mappableField.getJsonPathToProperty());
    addOverride(
        baseNode,
        jsonPathOfField,
        configurationRuntimeContext.getValue(mappableField.getContextKey()));
  }

  public static class GenericLookupYamlConfigurationFactoryFactory<T>
      extends DefaultConfigurationFactoryFactory<T> {
    @Override
    public ConfigurationFactory<T> create(
        Class<T> klass, Validator validator, ObjectMapper objectMapper, String propertyPrefix) {
      return new GenericLookupYamlConfigurationFactory<>(
          klass,
          validator,
          objectMapper,
          propertyPrefix,
          new JacksonTypeScanner(objectMapper, DROPWIZARD_PLAIN_TYPES),
          ConfigurationRuntimeContext.FROM_SYSTEM_PROPERTIES_AND_ENVIRONMENT);
    }
  }
}
