package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.params.provider.Arguments.of;

import io.dropwizard.configuration.UndefinedEnvironmentVariableException;
import java.util.stream.Stream;
import org.apache.commons.text.TextStringBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(key = "TEST", value = "foobar")
@SetSystemProperty(key = "TEST_JSON_STRING", value = "foo\nbar")
class SystemPropertyAndEnvironmentSubstitutorTest {

  @ParameterizedTest
  @MethodSource
  void shouldReplaceProperty(
      String givenInput, String expectedReplacement, boolean expectedAltered) {
    final TextStringBuilder builder = new TextStringBuilder(givenInput);
    boolean altered =
        new SystemPropertyAndEnvironmentSubstitutor(false)
            .substitute(builder, 0, givenInput.length());
    assertThat(altered).isEqualTo(expectedAltered);
    assertThat(builder.build()).isEqualTo(expectedReplacement);
  }

  static Stream<Arguments> shouldReplaceProperty() {
    return Stream.of(
        // standard replacement
        of("this is a ${TEST}", "this is a foobar", true),
        // standard replacement (should not be used without strict)
        of("this is a ${UNKNOWN_PROPERTY}", "this is a ${UNKNOWN_PROPERTY}", false),
        // standard replacement, use default
        of("this is a ${UNKNOWN_PROPERTY:-default}", "this is a default", true),
        // toJsonString replacement
        of("myProperty: ${TEST_JSON_STRING | toJsonString}", "myProperty: \"foo\\nbar\"", true),
        // toJsonString, not replaced, not altered (should not be used without strict)
        of(
            "myProperty: ${UNKNOWN_PROPERTY | toJsonString}",
            "myProperty: ${UNKNOWN_PROPERTY | toJsonString}",
            false),
        // toJsonString default used
        of("myProperty: ${UNKNOWN_PROPERTY | toJsonString:-null}", "myProperty: null", true),
        of(
            "myProperty: ${UNKNOWN_PROPERTY | toJsonString :-\"default\"}",
            "myProperty: \"default\"",
            true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"this is a ${UNKNOWN_PROPERTY}", "this is a ${UNKNOWN_PROPERTY | toJsonString}"})
  void shouldThrowExceptionIfStrict(String givenInput) {
    final TextStringBuilder builder = new TextStringBuilder(givenInput);
    final SystemPropertyAndEnvironmentSubstitutor substitutor =
        new SystemPropertyAndEnvironmentSubstitutor(true);
    final int length = givenInput.length();
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
