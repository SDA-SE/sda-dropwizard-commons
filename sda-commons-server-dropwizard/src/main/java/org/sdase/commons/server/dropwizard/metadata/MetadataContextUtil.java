package org.sdase.commons.server.dropwizard.metadata;

import java.util.concurrent.Callable;
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
    var fromProperty = System.getProperty(environmentOrPropertyName);
    if (StringUtils.isNotBlank(fromProperty)) {
      return fromProperty;
    }
    String fromEnvironment = System.getenv(environmentOrPropertyName);
    if (StringUtils.isNotBlank(fromEnvironment)) {
      return fromEnvironment;
    }
    throw new KeyConfigurationMissingException(environmentOrPropertyName);
  }
}
