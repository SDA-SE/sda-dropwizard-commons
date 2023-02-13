package org.sdase.commons.server.dropwizard.metadata;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

class MetadataContextUtil {

  private MetadataContextUtil() {
    // no instance needed
  }

  static MetadataContext current() {
    return MetadataContextHolder.get();
  }

  static Runnable transferMetadataContext(Runnable runnable) {
    MetadataContext metadataContext = MetadataContextHolder.get();

    return () -> {
      try {
        MetadataContextHolder.set(metadataContext);
        runnable.run();
      } finally {
        MetadataContextHolder.clear();
      }
    };
  }

  static <V> Callable<V> transferMetadataContext(Callable<V> callable) {
    MetadataContext metadataContext = MetadataContextHolder.get();

    return () -> {
      try {
        MetadataContextHolder.set(metadataContext);
        return callable.call();
      } finally {
        MetadataContextHolder.clear();
      }
    };
  }

  static String keyFromConfiguration(String environmentOrPropertyName)
      throws KeyConfigurationMissingException {
    return determineConfiguredValue(environmentOrPropertyName)
        .orElseThrow(() -> new KeyConfigurationMissingException(environmentOrPropertyName));
  }

  static Set<String> metadataFields() {
    return determineConfiguredValue(MetadataContext.METADATA_FIELDS_ENVIRONMENT_VARIABLE)
        .map(MetadataContextUtil::toSet)
        .orElse(Set.of());
  }

  static DetachedMetadataContext merge(
      DetachedMetadataContext context1, DetachedMetadataContext context2) {
    Set<String> keys =
        Stream.concat(context1.keySet().stream(), context2.keySet().stream())
            .collect(Collectors.toSet());
    var result = new DetachedMetadataContext();
    for (String key : keys) {
      var normalizedValues1 = normalizeValues(context1.get(key));
      var normalizedValues2 = normalizeValues(context2.get(key));
      var newValues =
          Stream.concat(normalizedValues1, normalizedValues2)
              .distinct()
              .collect(Collectors.toList());
      result.put(key, newValues);
    }
    return result;
  }

  static DetachedMetadataContext mergeWithPreference(
      DetachedMetadataContext preferredContextData, DetachedMetadataContext secondaryContextData) {
    Set<String> keys =
        Stream.concat(
                preferredContextData.keySet().stream(), secondaryContextData.keySet().stream())
            .collect(Collectors.toSet());
    var result = new DetachedMetadataContext();
    for (String key : keys) {
      var preferredValues =
          normalizeValues(preferredContextData.get(key)).collect(Collectors.toList());
      if (!preferredValues.isEmpty()) {
        result.put(key, preferredValues);
      } else {
        var secondaryValues =
            normalizeValues(secondaryContextData.get(key)).collect(Collectors.toList());
        result.put(key, secondaryValues);
      }
    }
    return result;
  }

  private static Stream<String> normalizeValues(List<String> values) {
    if (values == null) {
      return Stream.of();
    }
    return values.stream().filter(StringUtils::isNotBlank);
  }

  private static Set<String> toSet(String commaDelimitedValues) {
    var values = Arrays.asList(commaDelimitedValues.split(","));
    return values.stream()
        .filter(StringUtils::isNotBlank)
        .map(String::trim)
        .collect(Collectors.toSet());
  }

  private static Optional<String> determineConfiguredValue(String environmentOrPropertyName) {
    var fromProperty = System.getProperty(environmentOrPropertyName);
    if (StringUtils.isNotBlank(fromProperty)) {
      return Optional.of(fromProperty);
    }
    String fromEnvironment = System.getenv(environmentOrPropertyName);
    if (StringUtils.isNotBlank(fromEnvironment)) {
      return Optional.of(fromEnvironment);
    }
    return Optional.empty();
  }
}
