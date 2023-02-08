package org.sdase.commons.server.dropwizard.metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

class MetadataContextDefaultMethodsTest {

  Map<String, List<String>> testData = new LinkedHashMap<>();

  MetadataContext metadataContext =
      new MetadataContext() {

        @Override
        public Set<String> keys() {
          return testData.keySet();
        }

        @Override
        public List<String> valuesByKey(String key) {
          return testData.get(key);
        }
      };

  @Test
  @ClearSystemProperty(key = "METADATA_KEY_DOES_NOT_EXIST")
  void shouldFailIfKeyIsNotConfigured() {
    assertThatExceptionOfType(KeyConfigurationMissingException.class)
        .isThrownBy(() -> metadataContext.valuesByKeyFromEnvironment("METADATA_KEY_DOES_NOT_EXIST"))
        .withMessageContaining("METADATA_KEY_DOES_NOT_EXIST")
        .extracting(KeyConfigurationMissingException::getUnknownConfigurationKey)
        .isEqualTo("METADATA_KEY_DOES_NOT_EXIST");
  }

  @Test
  @SetSystemProperty(key = "METADATA_KEY_DEFINED", value = "tenant-id")
  void shouldResolveConfiguredKeyFromProperties() {
    testData.put("tenant-id", List.of("tenant-1"));
    assertThat(metadataContext.valuesByKeyFromEnvironment("METADATA_KEY_DEFINED"))
        .isEqualTo(List.of("tenant-1"));
  }

  @Test
  @DisabledForJreRange(min = JRE.JAVA_16, disabledReason = "Environment is unmodifiable.")
  @SetEnvironmentVariable(key = "METADATA_KEY_DEFINED", value = "tenant-id")
  void shouldResolveConfiguredKeyFromEnvironment() {
    testData.put("tenant-id", List.of("tenant-1"));
    testData.put("tenant-company-id", List.of("tenant-1"));
    assertThat(metadataContext.valuesByKeyFromEnvironment("METADATA_KEY_DEFINED"))
        .isEqualTo(List.of("tenant-1"));
  }

  @Test
  @DisabledForJreRange(min = JRE.JAVA_16, disabledReason = "Environment is unmodifiable.")
  @SetSystemProperty(key = "METADATA_KEY_DEFINED", value = "tenant-id")
  @SetEnvironmentVariable(key = "METADATA_KEY_DEFINED", value = "tenant-company-id")
  void shouldResolveConfiguredKeyPreferablyFromSystemProperties() {
    testData.put("tenant-id", List.of("tenant-1"));
    assertThat(metadataContext.valuesByKeyFromEnvironment("METADATA_KEY_DEFINED"))
        .isEqualTo(List.of("tenant-1"));
  }
}
