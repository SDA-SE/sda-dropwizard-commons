package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LookupKeyAndOperatorsTest {

  @ParameterizedTest
  @MethodSource("testData")
  void shouldExtractKeyAndOperators(
      String givenKeyAndOperators, String expectedLookupKey, List<String> expectedOperators) {
    LookupKeyAndOperators actual = LookupKeyAndOperators.of(givenKeyAndOperators);
    assertThat(actual)
        .extracting(LookupKeyAndOperators::getKey, LookupKeyAndOperators::getOperators)
        .containsExactly(expectedLookupKey, expectedOperators);
  }

  @ParameterizedTest
  @MethodSource("testData")
  void shouldExtractLookupKey(String givenKeyAndOperators, String expectedLookupKey) {
    String actual = LookupKeyAndOperators.lookupKey(givenKeyAndOperators);
    assertThat(actual).isEqualTo(expectedLookupKey);
  }

  static Stream<Arguments> testData() {
    return Stream.of(
        of("FOO_KEY", "FOO_KEY", List.of()),
        of("FOO_KEY|toJsonString", "FOO_KEY", List.of("toJsonString")),
        of("FOO_KEY | toJsonString", "FOO_KEY", List.of("toJsonString")),
        of(" FOO_KEY|toJsonString ", "FOO_KEY", List.of("toJsonString")),
        of(
            " FOO_KEY|toLowerCase|toJsonString ",
            "FOO_KEY",
            List.of("toLowerCase", "toJsonString")));
  }
}
