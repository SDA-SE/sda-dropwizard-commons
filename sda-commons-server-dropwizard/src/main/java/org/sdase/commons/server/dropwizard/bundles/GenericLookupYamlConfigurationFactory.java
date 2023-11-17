package org.sdase.commons.server.dropwizard.bundles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.Validator;
import org.apache.commons.text.lookup.StringLookup;
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
  private final StringLookup lookup;

  /**
   * Creates a new configuration factory for the given class.
   *
   * @param configurationClass the configuration class
   * @param validator the validator to use
   * @param objectMapper the Jackson {@link ObjectMapper} to use
   * @param propertyPrefix the system property name prefix used by overrides
   * @param jacksonTypeScanner the scanner to derive available properties
   * @param lookup the {@link StringLookup} to lookup values for {@link
   *     MappableField#getEnvironmentVariableName()}
   */
  public GenericLookupYamlConfigurationFactory(
      Class<T> configurationClass,
      @Nullable Validator validator,
      ObjectMapper objectMapper,
      String propertyPrefix,
      JacksonTypeScanner jacksonTypeScanner,
      StringLookup lookup) {
    super(configurationClass, validator, objectMapper, propertyPrefix);
    this.configurationClass = configurationClass;
    this.jacksonTypeScanner = jacksonTypeScanner;
    this.lookup = lookup;
  }

  @Override
  protected T build(JsonNode node, String path) throws IOException, ConfigurationException {
    HashSet<String> contextKeys = findKeysInEnvironmentContext();
    List<MappableField> mappableFields = jacksonTypeScanner.scan(configurationClass);
    mappableFields.stream()
        .map(m -> m.expand(contextKeys))
        .flatMap(List::stream)
        .forEach(mappableField -> configure(node, mappableField));
    return super.build(node, path);
  }

  private HashSet<String> findKeysInEnvironmentContext() {
    // TODO this is probably too related to the given lookup so both may be a separate kind of
    //      properties resolve mechanism just for this purpose
    var contextKeys = new HashSet<>(System.getenv().keySet());
    contextKeys.addAll(
        System.getProperties().keySet().stream()
            .map(Object::toString)
            .collect(Collectors.toList()));
    return contextKeys;
  }

  private void configure(JsonNode baseNode, MappableField mappableField) {
    String jsonPathOfField = String.join(".", mappableField.getJsonPathToProperty());
    addOverride(
        baseNode, jsonPathOfField, lookup.lookup(mappableField.getEnvironmentVariableName()));
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
          new JacksonTypeScanner(objectMapper),
          new SystemPropertyAndEnvironmentLookup());
    }
  }
}
