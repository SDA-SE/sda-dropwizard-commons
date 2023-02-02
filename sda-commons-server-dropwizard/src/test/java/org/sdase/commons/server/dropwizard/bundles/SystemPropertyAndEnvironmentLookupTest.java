package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

class SystemPropertyAndEnvironmentLookupTest {

  @Test
  @SetSystemProperty(key = "FOO", value = "bar")
  void shouldLookupSystemProperties() {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup("FOO")).isEqualTo("bar");
  }

  @ParameterizedTest
  @ValueSource(strings = {"FOO", "FOO | toJsonString"})
  void shouldReturnNullForMissingSystemProperty(String given) {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup(given)).isNull();
  }

  @ParameterizedTest
  @SetSystemProperty(key = "FOO", value = "a\"string that\nneeds\\escaping")
  @ValueSource(
      strings = {
        "FOO | toJsonString",
        "FOO| toJsonString",
        "FOO | toJsonString ",
        "FOO |toJsonString",
        "FOO|toJsonString",
        "FOO | TOJSONSTRING",
        "FOO| TOJSONSTRING",
        "FOO | TOJSONSTRING ",
        "FOO |TOJSONSTRING",
        "FOO|TOJSONSTRING",
        "FOO | tojsonstring",
        "FOO| tojsonstring",
        "FOO | tojsonstring ",
        "FOO |tojsonstring",
        "FOO|tojsonstring"
      })
  void shouldLookupSystemPropertiesWithEscaping(String propertyDeclaration) {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup(propertyDeclaration))
        .isEqualTo("\"a\\\"string that\\nneeds\\\\escaping\"");
  }

  @DisabledForJreRange(min = JRE.JAVA_16)
  @Test
  @SetEnvironmentVariable(key = "FOO", value = "bar")
  void shouldLookupEnvironmentVariables() {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup("FOO")).isEqualTo("bar");
  }

  @DisabledForJreRange(min = JRE.JAVA_16)
  @Test
  @SetEnvironmentVariable(key = "FOO", value = "bar_env")
  @SetSystemProperty(key = "FOO", value = "bar_prop")
  void shouldPreferSystemProperties() {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup("FOO")).isEqualTo("bar_prop");
  }
}
