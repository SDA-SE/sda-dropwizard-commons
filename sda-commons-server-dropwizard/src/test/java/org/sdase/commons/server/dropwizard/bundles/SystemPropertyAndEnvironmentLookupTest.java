package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

class SystemPropertyAndEnvironmentLookupTest {

  @Test
  @SetSystemProperty(key = "FOO", value = "bar")
  void shouldLookupSystemProperties() {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup("FOO")).isEqualTo("bar");
  }

  @Test
  @SetEnvironmentVariable(key = "FOO", value = "bar")
  void shouldLookupEnvironmentVariables() {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup("FOO")).isEqualTo("bar");
  }

  @Test
  @SetEnvironmentVariable(key = "FOO", value = "bar_env")
  @SetSystemProperty(key = "FOO", value = "bar_prop")
  void shouldPreferSystemProperties() {
    assertThat(new SystemPropertyAndEnvironmentLookup().lookup("FOO")).isEqualTo("bar_prop");
  }
}
