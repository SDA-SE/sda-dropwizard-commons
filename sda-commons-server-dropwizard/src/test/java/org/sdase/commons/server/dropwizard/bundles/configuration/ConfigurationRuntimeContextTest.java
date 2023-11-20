package org.sdase.commons.server.dropwizard.bundles.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.dropwizard.bundles.configuration.ConfigurationRuntimeContext.FROM_ENVIRONMENT;
import static org.sdase.commons.server.dropwizard.bundles.configuration.ConfigurationRuntimeContext.FROM_SYSTEM_PROPERTIES;
import static org.sdase.commons.server.dropwizard.bundles.configuration.ConfigurationRuntimeContext.FROM_SYSTEM_PROPERTIES_AND_ENVIRONMENT;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

class ConfigurationRuntimeContextTest {

  public static final String GIVEN_CONTEXT_KEY = "THE_CONTEXT_KEY";

  @Test
  @SetEnvironmentVariable(key = GIVEN_CONTEXT_KEY, value = "bar")
  @DisabledForJreRange(min = JRE.JAVA_16)
  void shouldReadFromEnv() {
    var actual = FROM_ENVIRONMENT.getValue(GIVEN_CONTEXT_KEY);
    assertThat(actual).isEqualTo("bar");
  }

  @Test
  @SetEnvironmentVariable(key = GIVEN_CONTEXT_KEY, value = "bar")
  @DisabledForJreRange(min = JRE.JAVA_16)
  void shouldReadFromEnvWhenNotInProperties() {
    var actual = FROM_SYSTEM_PROPERTIES_AND_ENVIRONMENT.getValue(GIVEN_CONTEXT_KEY);
    assertThat(actual).isEqualTo("bar");
  }

  @Test
  @SetSystemProperty(key = GIVEN_CONTEXT_KEY, value = "properties")
  @SetEnvironmentVariable(key = GIVEN_CONTEXT_KEY, value = "env")
  @DisabledForJreRange(min = JRE.JAVA_16)
  void shouldReadFromPropertiesWithPrecedence() {
    var actual = FROM_SYSTEM_PROPERTIES_AND_ENVIRONMENT.getValue(GIVEN_CONTEXT_KEY);
    assertThat(actual).isEqualTo("properties");
  }

  @Test
  @SetSystemProperty(key = GIVEN_CONTEXT_KEY, value = "bar")
  void shouldReadFromProperties() {
    var actual = FROM_SYSTEM_PROPERTIES.getValue(GIVEN_CONTEXT_KEY);
    assertThat(actual).isEqualTo("bar");
  }

  @Test
  @SetSystemProperty(key = GIVEN_CONTEXT_KEY + "_PROPERTY", value = "bar")
  @SetEnvironmentVariable(key = GIVEN_CONTEXT_KEY + "_ENV", value = "env")
  @SetSystemProperty(key = GIVEN_CONTEXT_KEY, value = "bar")
  @SetEnvironmentVariable(key = GIVEN_CONTEXT_KEY, value = "env")
  @DisabledForJreRange(min = JRE.JAVA_16)
  void shouldReturnCombinedKeys() {
    var actual = FROM_SYSTEM_PROPERTIES_AND_ENVIRONMENT.getDefinedKeys();
    assertThat(actual)
        .contains("THE_CONTEXT_KEY", "THE_CONTEXT_KEY_PROPERTY", "THE_CONTEXT_KEY_ENV");
  }
}
