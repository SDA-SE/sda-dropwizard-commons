package org.sdase.commons.server.dropwizard.bundles.scanner;

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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacksonTypeScanner {

  private static final Set<Class<?>> DEFAULT_PLAIN_TYPES =
      Set.of(String.class, File.class, Duration.class, DataSize.class);

  private static final Logger LOG = LoggerFactory.getLogger(JacksonTypeScanner.class);
  static final String MAP_STRING_KEY_NAME = "<key>";

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
}
