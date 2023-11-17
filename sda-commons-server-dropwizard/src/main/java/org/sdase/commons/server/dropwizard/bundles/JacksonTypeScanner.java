package org.sdase.commons.server.dropwizard.bundles;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.util.DataSize;
import io.dropwizard.util.Duration;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacksonTypeScanner {

  private static final Set<Class<?>> DEFAULT_PLAIN_TYPES =
      Set.of(String.class, File.class, Duration.class, DataSize.class);

  private static final Logger LOG = LoggerFactory.getLogger(JacksonTypeScanner.class);
  public static final String MAP_STRING_KEY_NAME = "<key>";

  private final ObjectMapper mapper;
  private final Set<Class<?>> plainTypes;

  public JacksonTypeScanner(ObjectMapper mapper) {
    this.mapper = mapper;
    plainTypes = DEFAULT_PLAIN_TYPES;
  }

  public List<MappableField> scan(Type rootType) {
    JavaType javaType = mapper.constructType(rootType);
    return scan(List.of(), javaType);
  }

  public String createConfigurationHints(Type rootType) {
    return scan(rootType).stream()
        .map(t -> t.getEnvironmentVariableName() + " (" + t.createTypeDescription() + ")")
        .sorted()
        .collect(Collectors.joining(System.lineSeparator()));
  }

  private List<MappableField> scan(List<String> rootPath, JavaType type) {
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
        } else if (propertyType.isArrayType()) {
          LOG.info("Found array type for {}. Arrays are not supported yet.", newRootPath);
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

  private Collection<MappableField> createMapFields(
      JavaType propertyType, List<String> newRootPath) {
    try {
      if (isTypedMap(propertyType, String.class, String.class)) {
        var actualRootPath = new ArrayList<>(newRootPath);
        actualRootPath.add(MAP_STRING_KEY_NAME);
        return List.of(new MappableField(actualRootPath, propertyType.getRawClass()));
      }
    } catch (Exception e) {
      LOG.debug("Failed to identify types of {} in {}", propertyType.getRawClass(), newRootPath);
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

  public static class MappableField {
    private final List<String> jsonPathToProperty;
    private final Type propertyType;
    private final String environmentVariableName;
    private final String propertyTypeSimpleName;

    public MappableField(List<String> jsonPathToProperty, Type propertyType) {
      this.jsonPathToProperty = jsonPathToProperty;
      this.propertyType = propertyType;
      this.environmentVariableName =
          jsonPathToProperty.stream().map(String::toUpperCase).collect(Collectors.joining("_"));
      this.propertyTypeSimpleName =
          (propertyType instanceof Class)
              ? ((Class<?>) propertyType).getSimpleName()
              : propertyType.getTypeName();
    }

    private MappableField(
        List<String> jsonPathToProperty, String environmentVariableName, Type propertyType) {
      this.jsonPathToProperty = jsonPathToProperty;
      this.propertyType = propertyType;
      this.environmentVariableName = environmentVariableName;
      this.propertyTypeSimpleName =
          (propertyType instanceof Class)
              ? ((Class<?>) propertyType).getSimpleName()
              : propertyType.getTypeName();
    }

    public List<String> getJsonPathToProperty() {
      return jsonPathToProperty;
    }

    public Type getPropertyType() {
      return propertyType;
    }

    public String getPropertyTypeSimpleName() {
      return propertyTypeSimpleName;
    }

    public String createTypeDescription() {
      if (propertyType instanceof Class) {
        var clazz = (Class<?>) propertyType;
        if (clazz.isEnum()) {
          return "enum " + getPropertyTypeSimpleName();
        }
        if (Map.class.isAssignableFrom(clazz)) {
          return "Map [not supported]";
        }
      }
      return getPropertyTypeSimpleName();
    }

    public String getEnvironmentVariableName() {
      return environmentVariableName;
    }

    /**
     * Resolves {@code MappableFields} from {@link #getEnvironmentVariableName()} that are available
     * in the current context, considering variable parts like {@value
     * JacksonTypeScanner#MAP_STRING_KEY_NAME}.
     *
     * @param availableKeysInContext the keys that are defined in the current context (e.g.
     *     environment variable names)
     * @return the {@code MappableField}s that are available in the current context, may be empty.
     *     <strong>Note that the type is just copied and might be wrong for the actual
     *     property.</strong>
     */
    public List<MappableField> expand(Set<String> availableKeysInContext) {
      if (availableKeysInContext.contains(getEnvironmentVariableName())) {
        return List.of(new MappableField(this.jsonPathToProperty, this.propertyType));
      }
      if (getEnvironmentVariableName()
          .contains(JacksonTypeScanner.MAP_STRING_KEY_NAME.toUpperCase())) {
        Pattern expandPattern = createExpandPattern(getEnvironmentVariableName());
        return availableKeysInContext.stream()
            .map(expandPattern::matcher)
            .filter(Matcher::matches)
            .map(
                matcher -> {
                  var newEnvironmentVariableName = matcher.group(0);
                  var newPath = new ArrayList<>(getJsonPathToProperty());
                  for (int i = 0; i < matcher.groupCount(); i++) {
                    var realKeyPart = matcher.group(i + 1);
                    for (int j = 0; j < newPath.size(); j++) {
                      if (JacksonTypeScanner.MAP_STRING_KEY_NAME.equals(newPath.get(j))) {
                        newPath.set(j, realKeyPart);
                        break;
                      }
                    }
                  }
                  return new MappableField(newPath, newEnvironmentVariableName, getPropertyType());
                })
            .collect(Collectors.toList());
      }
      return List.of();
    }

    private Pattern createExpandPattern(String environmentVariableName) {
      return Pattern.compile(
          "^"
              + environmentVariableName.replace(
                  JacksonTypeScanner.MAP_STRING_KEY_NAME.toUpperCase(), "(.*)")
              + "$");
    }
  }
}
