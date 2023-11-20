package org.sdase.commons.server.dropwizard.bundles.scanner;

import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH;
import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.MAP_KEY_PLACE_HOLDER_IN_CONTEXT_KEY;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a field in a configuration class that can be set from a property in the context. The
 * context is a runtime configuration of key value pairs like {@link System#getenv()} or {@link
 * System#getProperties()}.
 */
public class MappableField {
  private final List<String> jsonPathToProperty;
  private final Type propertyType;
  private final String contextKey;
  private final String propertyTypeDescription;

  /**
   * Creates a new {@code MappableField} with internals derived from the given arguments.
   *
   * @param jsonPathToProperty The Json path to the mappable field, derived from the defined
   *     structure in the configuration class. Each element represents a path segment or in other
   *     words a property of a class in the configuration class structure. The path considers the
   *     Json representation instead of the actual Java property name.
   * @param propertyType The type of the property to provide information for mapping and
   *     documentation.
   */
  MappableField(List<String> jsonPathToProperty, Type propertyType) {
    this(
        jsonPathToProperty,
        jsonPathToProperty.stream().map(String::toUpperCase).collect(Collectors.joining("_")),
        propertyType);
  }

  private MappableField(List<String> jsonPathToProperty, String contextKey, Type propertyType) {
    this.jsonPathToProperty = jsonPathToProperty;
    this.propertyType = propertyType;
    this.contextKey = contextKey;
    this.propertyTypeDescription = createPropertyTypeDescription(propertyType);
  }

  /**
   * @return The Json path to the mappable field, derived from the defined structure in the
   *     configuration class. Each element represents a path segment or in other words a property of
   *     a class in the configuration class structure. The path considers the Json representation
   *     instead of the actual Java property name. It may contain elements that are placeholders if
   *     this {@link MappableField} is not {@linkplain #expand(Set) expanded} yet. Expanded {@link
   *     MappableField}s are reduced to the actually available configuration.
   */
  public List<String> getJsonPathToProperty() {
    return jsonPathToProperty;
  }

  /**
   * @return A short description of the type that provides a hint for the format of the value.
   */
  public String getPropertyTypeDescription() {
    return this.propertyTypeDescription;
  }

  /**
   * @return The key in the configuration runtime context (e.g. environment variables or system
   *     properties) that represents this {@link MappableField}. The {@code contextKey} may contain
   *     placeholders if this {@link MappableField} is not {@linkplain #expand(Set) expanded} yet.
   *     Expanded {@link MappableField}s are reduced to the actually available configuration.
   */
  public String getContextKey() {
    return contextKey;
  }

  /**
   * Resolves {@code MappableFields} from {@link #getContextKey()} that are available in the current
   * context, considering variable parts like {@value
   * JacksonTypeScanner#MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH}.
   *
   * @param availableKeysInContext the keys that are defined in the current configuration runtime
   *     context (e.g. environment variables or system properties)
   * @return the {@code MappableField}s that are available in the current context, may be empty.
   *     <strong>Note that the type is just copied and might be wrong for the actual
   *     property.</strong>
   */
  public List<MappableField> expand(Set<String> availableKeysInContext) {
    if (availableKeysInContext.contains(getContextKey())) {
      return List.of(new MappableField(this.jsonPathToProperty, this.propertyType));
    }
    if (getContextKey().contains(MAP_KEY_PLACE_HOLDER_IN_CONTEXT_KEY)) {
      Pattern expandPattern = createExpandPattern(getContextKey());
      return availableKeysInContext.stream()
          .map(expandPattern::matcher)
          .filter(Matcher::matches)
          .map(this::createMappableFieldFromContextMatch)
          .collect(Collectors.toList());
    }
    return List.of();
  }

  /**
   * @param contextKeyMatch a {@code Matcher} created from a {@link Pattern} for the {@linkplain
   *     #getContextKey() context variable} with placeholders that {@link Matcher#matches()} an
   *     existing key in the context
   * @return a {@link MappableField} that represents the actual key in the context and the final
   *     {@linkplain #getJsonPathToProperty() Json path to the configuration property}
   * @see #expand(Set)
   */
  private MappableField createMappableFieldFromContextMatch(Matcher contextKeyMatch) {
    var newEnvironmentVariableName = contextKeyMatch.group(0);
    var newPath = new ArrayList<>(getJsonPathToProperty());
    for (int i = 0; i < contextKeyMatch.groupCount(); i++) {
      var realKeyPart = contextKeyMatch.group(i + 1);
      for (int j = 0; j < newPath.size(); j++) {
        if (MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH.equals(newPath.get(j))) {
          newPath.set(j, realKeyPart);
          break;
        }
      }
    }
    return new MappableField(newPath, newEnvironmentVariableName, propertyType);
  }

  private String createPropertyTypeDescription(Type type) {
    if (type instanceof Class) {
      var clazz = (Class<?>) type;
      if (clazz.isEnum()) {
        return "enum " + clazz.getSimpleName();
      }
      if (Map.class.isAssignableFrom(clazz)) {
        return "Map";
      }
      return clazz.getSimpleName();
    }
    return type.getTypeName();
  }

  private Pattern createExpandPattern(String environmentVariableName) {
    return Pattern.compile(
        "^" + environmentVariableName.replace(MAP_KEY_PLACE_HOLDER_IN_CONTEXT_KEY, "(.*)") + "$");
  }
}
