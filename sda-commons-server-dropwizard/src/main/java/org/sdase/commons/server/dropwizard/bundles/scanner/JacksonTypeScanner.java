package org.sdase.commons.server.dropwizard.bundles.scanner;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;
import java.io.File;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans configuration classes for configurable properties recursively (up to {@value MAX_DEPTH}
 * nesting depth). Property names are derived from the Json representation according the used {@link
 * ObjectMapper} instead of the Java property names. Dynamic parts like keys of maps are represented
 * by placeholders.
 *
 * <p>This implementation is not feature complete. There are some types that are not considered
 * mappable yet.
 */
public class JacksonTypeScanner {

  /** Types that can be mapped from {@code String} in Dropwizard configuration. */
  public static final Set<Class<?>> DROPWIZARD_PLAIN_TYPES =
      Set.of(String.class, File.class, Duration.class, DataSize.class, URI.class, URL.class);

  /**
   * The maximum nested depth of properties. This limits recursion when scanning for properties to
   * avoid stack-overflows.
   */
  static final int MAX_DEPTH = 25;

  /** The placeholder used in property paths for keys of a {@code Map}. */
  static final String MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH = "<key>";

  /** The placeholder used in context keys for keys of a {@code Map}. */
  static final String MAP_KEY_PLACEHOLDER_IN_CONTEXT_KEY =
      MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH.toUpperCase();

  /** The placeholder used in property paths for item indexes of an array. */
  static final String ARRAY_INDEX_PLACEHOLDER_IN_CONFIGURATION_PATH = "<index>";

  /** The placeholder used in context keys for item indexes of an array. */
  static final String ARRAY_INDEX_PLACEHOLDER_IN_CONTEXT_KEY =
      ARRAY_INDEX_PLACEHOLDER_IN_CONFIGURATION_PATH.toUpperCase();

  /** The placeholder used in property paths for item indexes of an array. */
  static final String JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH = "<any>";

  /** The placeholder used in context keys for item indexes of an array. */
  static final String JSON_NODE_PLACEHOLDER_IN_CONTEXT_KEY =
      JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH.toUpperCase();

  private final ObjectMapper mapper;
  private final Set<Class<?>> plainTypes;

  private static final Logger LOG = LoggerFactory.getLogger(JacksonTypeScanner.class);

  /**
   * @param mapper the {@link ObjectMapper} used for mapping the property names to expected name
   *     parts in the configuration.
   * @param plainTypes Types of properties that can be configured with a plain {@code String} value.
   *     Which types are configurable from {@code String} values depends on the given {@code
   *     objectMapper} and the implementation of the actual mapping from a context value to the
   *     configuration property.
   */
  public JacksonTypeScanner(ObjectMapper mapper, Set<Class<?>> plainTypes) {
    this.mapper = mapper;
    this.plainTypes = plainTypes;
  }

  /**
   * Scans the given {@code rootType} for configurable fields recursively.
   *
   * @param rootType The root type of the configuration class that has configurable properties.
   * @return All properties that are configurable with information about (Json) mapping, type,
   *     minimal documentation and expected property key in the configured runtime context. The
   *     returned fields may define patterns with placeholders that are not found as is. When
   *     {@linkplain MappableField#expand(Set) expanded} for a defined context, these fields can be
   *     used to derive and map values to the configuration.
   */
  public List<MappableField> scan(Type rootType) {
    JavaType javaType = mapper.constructType(rootType);
    return scan(List.of(), javaType);
  }

  /**
   * Creates a multi-line text that describes each found property briefly.
   *
   * @param rootType The root type of the configuration class that has configurable properties.
   * @return human readable text for minimal documentation.
   */
  public String createConfigurationHints(Type rootType) {
    return scan(rootType).stream()
        .map(t -> t.getContextKey() + " (" + t.getPropertyTypeDescription() + ")")
        .sorted()
        .collect(Collectors.joining(System.lineSeparator()));
  }

  private List<MappableField> scan(List<String> rootPath, JavaType type) {
    if (rootPath.size() > MAX_DEPTH) {
      LOG.warn(
          "Parsed configuration class is too nested for further override evaluation at path {}",
          rootPath);
      return List.of();
    }
    try {
      BeanDescription introspect = mapper.getSerializationConfig().introspect(type);
      List<MappableField> fields = new ArrayList<>();
      for (var property : introspect.findProperties()) {
        String name = property.getName();
        var newRootPath = new ArrayList<>(rootPath);
        newRootPath.add(name);
        JavaType propertyType = getDeclaredType(property.getPrimaryType());
        if (propertyType == null) {
          LOG.info("Could not find type of field at path {}", newRootPath);
          continue;
        }
        if (propertyType.isPrimitive()
            || propertyType.isEnumType()
            || propertyType.isEnumImplType()
            || isPlainType(propertyType)) {
          fields.add(new MappableField(newRootPath, propertyType.getRawClass()));
        } else if (JsonNode.class.equals(propertyType.getRawClass())) {
          newRootPath.add(JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH);
          fields.add(new MappableField(newRootPath, propertyType.getRawClass()));
        } else if (propertyType.isArrayType() || propertyType.isCollectionLikeType()) {
          fields.addAll(createArrayFields(propertyType, newRootPath));
        } else if (Map.class.isAssignableFrom(propertyType.getRawClass())) {
          fields.addAll(createMapFields(propertyType, newRootPath));
        } else {
          fields.addAll(scan(newRootPath, propertyType));
        }
      }
      return fields;
    } catch (Exception e) {
      LOG.info("Could not parse config class {} for dynamic environment properties.", type, e);
      return List.of();
    }
  }

  private Collection<MappableField> createArrayFields(
      JavaType propertyType, ArrayList<String> arrayRootPath) {
    var newRootPath = new ArrayList<>(arrayRootPath);
    newRootPath.add(ARRAY_INDEX_PLACEHOLDER_IN_CONFIGURATION_PATH);

    JavaType itemType = identifyItemTypeOfArrayType(propertyType);

    if (itemType == null) {
      LOG.debug(
          "Failed to identify item type of {} in {}", propertyType.getRawClass(), arrayRootPath);
      return List.of();
    }

    if (isPlainType(itemType)) {
      return List.of(new MappableField(newRootPath, propertyType.getRawClass()));
    }
    if (JsonNode.class.equals(itemType.getRawClass())) {
      newRootPath.add(JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH);
      return List.of(new MappableField(newRootPath, propertyType.getRawClass()));
    }
    if (itemType.isArrayType() || itemType.isCollectionLikeType()) {
      return createArrayFields(itemType, newRootPath);
    }
    if (Map.class.isAssignableFrom(itemType.getRawClass())) {
      return createMapFields(itemType, newRootPath);
    }

    return scan(newRootPath, itemType);
  }

  private JavaType identifyItemTypeOfArrayType(JavaType propertyType) {
    var rawClass = propertyType.getRawClass();
    if (rawClass.isArray()) {
      return mapper.constructType(rawClass.getComponentType());
    } else if (Collection.class.isAssignableFrom(propertyType.getRawClass())) {
      return propertyType.findTypeParameters(Collection.class)[0];
    }
    return null;
  }

  private Collection<MappableField> createMapFields(
      JavaType propertyType, List<String> mapRootPath) {
    try {
      var newRootPath = new ArrayList<>(mapRootPath);
      newRootPath.add(MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH);
      if (isTypedMap(propertyType, String.class, String.class)) {
        return List.of(new MappableField(newRootPath, propertyType.getRawClass()));
      }
      if (isTypedMap(propertyType, String.class, JsonNode.class)) {
        newRootPath.add(JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH);
        return List.of(new MappableField(newRootPath, JsonNode.class));
      }
      if (isTypedMap(propertyType, String.class, Object.class)) {
        return scan(newRootPath, propertyType.findTypeParameters(Map.class)[1]);
      }
    } catch (Exception e) {
      LOG.debug("Failed to identify types of {} in {}", propertyType.getRawClass(), mapRootPath);
    }
    return List.of();
  }

  private boolean isTypedMap(JavaType propertyType, Class<?> keyType, Class<?> valueType) {
    JavaType[] typeParameters = propertyType.findTypeParameters(Map.class);
    return keyType.isAssignableFrom(typeParameters[0].getRawClass())
        && valueType.isAssignableFrom(typeParameters[1].getRawClass());
  }

  /**
   * Checks if the identified type of property is a plain type. A plain type can be expressed as a
   * single value in contrast to an object that itself has multiple properties or a list that has
   * multiple values. An object type can be a plain type, if it is deserializable from a single
   * value, like a {@link Duration} is created from {@code 50ms}.
   *
   * @param propertyType the type of the property to check.
   * @return if {@code propertyType} represents a plain type
   */
  private boolean isPlainType(JavaType propertyType) {
    return plainTypes.stream().anyMatch(propertyType::hasRawClass);
  }

  private JavaType getDeclaredType(JavaType primaryType) {
    if (Optional.class.isAssignableFrom(primaryType.getRawClass())) {
      return getDeclaredType(primaryType.findTypeParameters(Optional.class)[0]);
    }
    if (primaryType.getRawClass().isAnnotationPresent(JsonTypeInfo.class)) {
      var jsonTypeInfo = primaryType.getRawClass().getAnnotation(JsonTypeInfo.class);
      if (jsonTypeInfo.defaultImpl() != null) {
        return mapper.constructType(jsonTypeInfo.defaultImpl());
      }
    }
    return primaryType;
  }
}
