package org.sdase.commons.server.dropwizard.metadata;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
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
