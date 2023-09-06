package org.sdase.commons.server.dropwizard.bundles;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.dropwizard.bundles.JacksonTypeScanner.MappableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO may be not needed any more
 *
 * <p>This implementation supports to fill Maps from a JSON String in the environment. The official
 * <a href
 * ="https://dropwizardio.readthedocs.io/en/latest/manual/core.html#man-core-configuration">config
 * override syntax</a> just uses the keys, but they could be hard to map from environment variables
 * in upper case. It could also get complicated because maps could have different types as values.
 *
 * @param <T>
 */
public class GenericEnvironmentMapper<T> implements ConfigurationSourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GenericEnvironmentMapper.class);

  private final ConfigurationSourceProvider delegate;
  private final Class<T> configurationClass;
  // as in io.dropwizard.configuration.YamlConfigurationFactory
  private final YAMLFactory yamlFactory = new YAMLFactory();
  private final ObjectMapper mapper =
      new ObjectMapper(yamlFactory).disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
  private final JacksonTypeScanner jacksonTypeScanner = new JacksonTypeScanner(mapper);
  private final SystemPropertyAndEnvironmentLookup systemPropertyAndEnvironmentLookup =
      new SystemPropertyAndEnvironmentLookup();

  /**
   * Create a new instance.
   *
   * @param delegate The underlying {@link ConfigurationSourceProvider}.
   * @param configurationClass the class where the configuration is mapped to afterwards.
   */
  public GenericEnvironmentMapper(
      ConfigurationSourceProvider delegate, Class<T> configurationClass) {
    this.delegate = delegate;
    this.configurationClass = configurationClass;
  }

  @Override
  public InputStream open(String path) throws IOException {
    var jsonFromDelegate = jsonObjectFromDelegate(path);
    var fields = jacksonTypeScanner.scan(this.configurationClass);

    var configObjectFromEnvironment = buildEnvironmentConfig(fields);

    Object finalConfig =
        mapper
            .readerForUpdating(jsonFromDelegate)
            .readValue(mapper.writeValueAsString(configObjectFromEnvironment));

    return new ByteArrayInputStream(mapper.writeValueAsBytes(finalConfig));
  }

  private Object buildEnvironmentConfig(List<MappableField> fields) {
    ObjectNode objectNode = mapper.createObjectNode();
    for (var field : fields) {
      String value = systemPropertyAndEnvironmentLookup.lookup(field.getEnvironmentVariableName());
      if (value != null) {
        addValue(objectNode, field.getJsonPathToProperty(), field.getPropertyType(), value);
      }
    }
    return objectNode;
  }

  private void addValue(ObjectNode target, List<String> jsonPath, Type type, String value) {
    ObjectNode currentTarget = target;
    for (int i = 0; i < jsonPath.size(); i++) {
      String currentName = jsonPath.get(i);
      if (i == jsonPath.size() - 1) {
        putValue(currentTarget, currentName, value, type);
      } else {
        ObjectNode nextCurrentTarget = findNextCurrentTarget(currentTarget, currentName, jsonPath);
        if (nextCurrentTarget != null) {
          currentTarget = nextCurrentTarget;
        } else {
          return;
        }
      }
    }
  }

  private void putValue(
      ObjectNode target, String propertyName, String valueFromEnv, Type targetType) {
    if (targetType.equals(Map.class)) {
      try {
        target.putPOJO(propertyName, mapper.readValue(valueFromEnv, ObjectNode.class));
      } catch (JsonProcessingException e) {
        LOG.warn("Failed to read object for {}", propertyName, e);
      }
      // TODO boolean, number, etc. see JacksonTypeScannerTest
    } else {
      target.put(propertyName, valueFromEnv);
    }
  }

  private ObjectNode findNextCurrentTarget(
      ObjectNode currentTarget, String propertyName, List<String> jsonPath) {
    JsonNode existingNode = currentTarget.get(propertyName);
    if (existingNode != null) {
      if (!existingNode.isObject()) {
        LOG.warn(
            "Can't create {} of {}. Already exists as {}",
            propertyName,
            jsonPath,
            existingNode.getNodeType());
        return null;
      }
      return ((ObjectNode) existingNode);
    } else {
      return currentTarget.putObject(propertyName);
    }
  }

  private Object jsonObjectFromDelegate(String path) {
    try (var substituted = delegate.open(path)) {
      return mapper.readValue(substituted, Object.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
