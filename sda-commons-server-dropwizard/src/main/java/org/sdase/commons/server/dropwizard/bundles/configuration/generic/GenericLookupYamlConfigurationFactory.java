package org.sdase.commons.server.dropwizard.bundles.configuration.generic;

import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.DROPWIZARD_PLAIN_TYPES;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.configuration.YamlConfigurationFactory;
import jakarta.annotation.Nullable;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.text.lookup.StringLookup;
import org.sdase.commons.server.dropwizard.bundles.configuration.ConfigurationRuntimeContext;
import org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner;
import org.sdase.commons.server.dropwizard.bundles.scanner.MappableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger LOG =
      LoggerFactory.getLogger(GenericLookupYamlConfigurationFactory.class);

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
        .sorted()
        .forEach(mappableField -> configure(node, mappableField));
    return super.build(node, path);
  }

  private void configure(JsonNode baseNode, MappableField mappableField) {
    String value = configurationRuntimeContext.getValue(mappableField.getContextKey());
    try {
      String jsonPathOfField =
          mappableField.getJsonPathToProperty().stream()
              // Dropwizard override convention: field names with . must be escaped
              .map(p -> p.replace(".", "\\."))
              .collect(Collectors.joining("."));
      addOverride(baseNode, jsonPathOfField, value);
    } catch (IllegalArgumentException e) {
      LOG.info(
          "{} is not mappable with Dropwizard overrides, trying best guess approach",
          mappableField,
          e);
      addCustomOverride(baseNode, mappableField.getJsonPathToProperty(), value);
    }
  }

  private void addCustomOverride(JsonNode rootNode, List<String> path, String value) {
    try {
      if (path.isEmpty()) {
        return;
      }
      var propertyName = path.get(0);
      var valueNow = path.size() == 1;

      // setting values
      if (valueNow) {
        addValue(rootNode, value, propertyName);
        return;
      }

      // building the hierarchy
      JsonNode nextNode = addNextNodeInHierarchy(rootNode, path, propertyName);
      addCustomOverride(nextNode, path.subList(1, path.size()), value);

    } catch (Exception e) {
      if (e instanceof IllegalArgumentException) {
        throw e;
      }
      LOG.error("Failed to create an override for {}", path, e);
    }
  }

  private JsonNode addNextNodeInHierarchy(
      JsonNode rootNode, List<String> path, String propertyName) {
    var representsArray = path.size() > 1 && isArrayIndexKey(path.get(1));
    if (rootNode.isObject()) {
      return setProperty(rootNode, propertyName, representsArray);
    } else if (rootNode.isArray() && isArrayIndexKey(propertyName)) {
      return insertArrayItemIfNotExists((ArrayNode) rootNode, propertyName, representsArray);
    } else {
      throw new IllegalArgumentException("Unable to build hierarchy.");
    }
  }

  private ContainerNode<? extends ContainerNode<?>> setProperty(
      JsonNode objectNodeTarget, String propertyName, boolean valueIsArray) {
    return valueIsArray
        ? objectNodeTarget.withArrayProperty(propertyName)
        : objectNodeTarget.withObjectProperty(propertyName);
  }

  private JsonNode insertArrayItemIfNotExists(
      ArrayNode targetArrayNode, String nextKey, boolean itemRepresentsArray) {
    int arrayIndex = Integer.parseInt(nextKey);
    var nextNode = targetArrayNode.get(arrayIndex);
    if (nextNode == null || nextNode.isNull() || nextNode.isMissingNode()) {
      nextNode =
          itemRepresentsArray
              ? targetArrayNode.insertArray(arrayIndex)
              : targetArrayNode.insertObject(arrayIndex);
    }
    return nextNode;
  }

  private void addValue(JsonNode rootNode, String value, String nextKey) {
    if (isArrayIndexKey(nextKey)) {
      if (!(rootNode instanceof ArrayNode)) {
        throw new IllegalArgumentException("Need to set array element, but node is not an array.");
      }
      int arrayIndex = Integer.parseInt(nextKey);
      ((ArrayNode) rootNode).insert(arrayIndex, value);
    } else {
      if (!(rootNode instanceof ObjectNode)) {
        throw new IllegalArgumentException("Need to set a property, but node is not an object.");
      }
      ((ObjectNode) rootNode).put(nextKey, value);
    }
  }

  private boolean isArrayIndexKey(String propertyKey) {
    return propertyKey.matches("^\\d+$");
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
