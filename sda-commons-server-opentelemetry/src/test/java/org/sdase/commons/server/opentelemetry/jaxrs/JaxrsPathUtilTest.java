package org.sdase.commons.server.opentelemetry.jaxrs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JaxrsPathUtilTest {

  @ParameterizedTest
  @MethodSource
  void shouldNormalize(String given, String expected) {
    assertThat(JaxrsPathUtil.normalizePath(given)).isEqualTo(expected);
  }

  static Stream<Arguments> shouldNormalize() {
    return Stream.of(
        of("/a-path", "/a-path"),
        of("a-path", "/a-path"),
        of("/a-path/", "/a-path"),
        of("a-path/", "/a-path"),
        of("", ""),
        of("/", ""),
        of(null, ""));
  }
}
