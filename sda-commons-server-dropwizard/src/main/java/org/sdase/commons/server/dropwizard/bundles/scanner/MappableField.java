package org.sdase.commons.server.dropwizard.bundles.scanner;

import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.ARRAY_INDEX_PLACEHOLDER_IN_CONFIGURATION_PATH;
import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.ARRAY_INDEX_PLACEHOLDER_IN_CONTEXT_KEY;
import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH;
import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.JSON_NODE_PLACEHOLDER_IN_CONTEXT_KEY;
import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH;
import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.MAP_KEY_PLACEHOLDER_IN_CONTEXT_KEY;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a field in a configuration class that can be set from a property in the context. The
 * context is a runtime configuration of key value pairs like {@link System#getenv()} or {@link
 * System#getProperties()}.
 *
 * <p>Note: This class has a natural ordering that is technically inconsistent with equals. While
 * {@link #equals(Object)} considers all fields, {@link #compareTo(MappableField)} only considers
 * the {@linkplain #getJsonPathToProperty() Json path to property}. {@code MappableFiel}s that are
 * discovered by {@linkplain JacksonTypeScanner} should not be affected by this inconsistency.
 */
public class MappableField implements Comparable<MappableField> {
  private static final Set<String> DIRECT_REPLACEMENT_PATH_PROPERTY_PLACEHOLDERS =
      Set.of(
          MAP_KEY_PLACEHOLDER_IN_CONFIGURATION_PATH,
          ARRAY_INDEX_PLACEHOLDER_IN_CONFIGURATION_PATH,
          JSON_NODE_PLACEHOLDER_IN_CONFIGURATION_PATH);
  private static final Set<String> CONTEXT_KEY_PLACEHOLDERS =
      Set.of(
          MAP_KEY_PLACEHOLDER_IN_CONTEXT_KEY,
          ARRAY_INDEX_PLACEHOLDER_IN_CONTEXT_KEY,
          JSON_NODE_PLACEHOLDER_IN_CONTEXT_KEY);

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
    if (CONTEXT_KEY_PLACEHOLDERS.stream().anyMatch(p -> getContextKey().contains(p))) {
      Pattern expandPattern = createExpandPattern(getContextKey());
      return availableKeysInContext.stream()
          .map(expandPattern::matcher)
          .filter(Matcher::matches)
          .map(this::createMappableFieldFromContextMatch)
          .collect(Collectors.toList());
    }
    return List.of();
  }

  @Override
  public String toString() {
    return "MappableField{" + getContextKey() + ' ' + getPropertyTypeDescription() + '}';
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
        if (DIRECT_REPLACEMENT_PATH_PROPERTY_PLACEHOLDERS.contains(newPath.get(j))) {
          newPath.set(j, realKeyPart);
          break;
        }
      }
    }
    var expandedNewPath =
        newPath.stream()
            .map(p -> p.split("_"))
            .flatMap(Stream::of)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    return new MappableField(expandedNewPath, newEnvironmentVariableName, propertyType);
  }

  private String createPropertyTypeDescription(Type type) {
    if (type instanceof Class<?> clazz) {
      if (clazz.isEnum()) {
        return "enum " + clazz.getSimpleName();
      }
      if (Map.class.isAssignableFrom(clazz)) {
        return "Map";
      }
      if (clazz.isArray() || Collection.class.isAssignableFrom(clazz)) {
        return "Array";
      }
      return clazz.getSimpleName();
    }
    return type.getTypeName();
  }

  private Pattern createExpandPattern(String environmentVariableName) {
    var envWithMapKeyPattern =
        environmentVariableName.replace(MAP_KEY_PLACEHOLDER_IN_CONTEXT_KEY, "(.*)");
    var envWithMapKeyAndArrayIndexPattern =
        envWithMapKeyPattern.replace(ARRAY_INDEX_PLACEHOLDER_IN_CONTEXT_KEY, "(\\d*)");
    var envWithMapKeyAndArrayIndexAndAnyPattern =
        envWithMapKeyAndArrayIndexPattern.replace(
            "_" + JSON_NODE_PLACEHOLDER_IN_CONTEXT_KEY, "(_.+|$)");
    return Pattern.compile("^" + envWithMapKeyAndArrayIndexAndAnyPattern + "$");
  }

  @Override
  public int compareTo(@NotNull MappableField o) {
    var minDepth = Math.min(this.getJsonPathToProperty().size(), o.getJsonPathToProperty().size());
    for (int i = 0; i < minDepth; i++) {
      var part = this.getJsonPathToProperty().get(i);
      var oPart = o.getJsonPathToProperty().get(i);
      int compare;
      if (part.matches("^\\d+$") && oPart.matches("^\\d+$")) {
        compare = Integer.compare(Integer.parseInt(part), Integer.parseInt(oPart));
      } else {
        compare = part.compareTo(oPart);
      }
      if (compare != 0) {
        return compare;
      }
    }
    return Integer.compare(this.getJsonPathToProperty().size(), o.getJsonPathToProperty().size());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MappableField that = (MappableField) o;

    if (!Objects.equals(jsonPathToProperty, that.jsonPathToProperty)) return false;
    if (!Objects.equals(propertyType, that.propertyType)) return false;
    if (!Objects.equals(contextKey, that.contextKey)) return false;
    return Objects.equals(propertyTypeDescription, that.propertyTypeDescription);
  }

  @Override
  public int hashCode() {
    int result = jsonPathToProperty != null ? jsonPathToProperty.hashCode() : 0;
    result = 31 * result + (propertyType != null ? propertyType.hashCode() : 0);
    result = 31 * result + (contextKey != null ? contextKey.hashCode() : 0);
    result =
        31 * result + (propertyTypeDescription != null ? propertyTypeDescription.hashCode() : 0);
    return result;
  }
}
