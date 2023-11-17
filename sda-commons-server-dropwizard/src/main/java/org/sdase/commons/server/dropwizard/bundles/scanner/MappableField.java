package org.sdase.commons.server.dropwizard.bundles.scanner;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MappableField {
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
        return "Map";
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
