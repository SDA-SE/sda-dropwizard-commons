package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.dropwizard.configuration.UndefinedEnvironmentVariableException;
import org.apache.commons.text.TextStringBuilder;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(key = "TEST", value = "foobar")
class SystemPropertyAndEnvironmentSubstitutorTest {

  @Test
  void shouldReplaceProperty() {
    final String input = "this is a ${TEST}";
    final TextStringBuilder builder = new TextStringBuilder(input);
    boolean altered =
        new SystemPropertyAndEnvironmentSubstitutor(false).substitute(builder, 0, input.length());
    assertThat(altered).isTrue();
    assertThat(builder.build()).isEqualTo("this is a foobar");
  }

  @Test
  void shouldThrowExceptionIfStrict() {
    final String input = "this is a ${UNKNOWN_PROPERTY}";
    final TextStringBuilder builder = new TextStringBuilder(input);
    final SystemPropertyAndEnvironmentSubstitutor substitutor =
        new SystemPropertyAndEnvironmentSubstitutor(true);
    final int length = input.length();
    assertThatCode(() -> substitutor.substitute(builder, 0, length))
        .isInstanceOf(UndefinedEnvironmentVariableException.class);
  }

  @Test
  void shouldNotThrowExceptionIfNotStrict() {
    final String input = "this is a ${UNKNOWN_PROPERTY}";
    final TextStringBuilder builder = new TextStringBuilder(input);
    final SystemPropertyAndEnvironmentSubstitutor substitutor =
        new SystemPropertyAndEnvironmentSubstitutor(false);
    final int length = input.length();
    assertThatCode(() -> substitutor.substitute(builder, 0, length)).doesNotThrowAnyException();
  }
}
