package org.sdase.commons.server.dropwizard.metadata;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.sdase.commons.server.dropwizard.metadata.MetadataContextMergeStrategy.EXTEND;
import static org.sdase.commons.server.dropwizard.metadata.MetadataContextMergeStrategy.KEEP;
import static org.sdase.commons.server.dropwizard.metadata.MetadataContextMergeStrategy.REPLACE;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MetadataContextMergeIntegrationTest {

  @ParameterizedTest
  @MethodSource
  void shouldMergeContext(
      Map<String, List<String>> currentContext,
      Map<String, List<String>> newContext,
      MetadataContextMergeStrategy strategy,
      Map<String, List<String>> expected) {
    var c = new DetachedMetadataContext();
    c.putAll(currentContext);
    MetadataContext.createContext(c);
    var n = new DetachedMetadataContext();
    if (newContext == null) {
      n = null;
    } else {
      n.putAll(newContext);
    }

    MetadataContext.mergeContext(n, strategy);

    var actual = MetadataContext.current();
    SoftAssertions.assertSoftly(
        softly -> {
          softly.assertThat(actual.keys()).containsExactlyInAnyOrderElementsOf(expected.keySet());
          expected
              .keySet()
              .forEach(k -> softly.assertThat(actual.valuesByKey(k)).isEqualTo(expected.get(k)));
        });
  }

  @Test
  void shouldRequireStrategy() {
    DetachedMetadataContext newContextData = new DetachedMetadataContext();
    //noinspection ConstantConditions
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> MetadataContext.mergeContext(newContextData, null));
  }

  static Stream<Arguments> shouldMergeContext() {
    return Stream.of(
        of(
            Map.of("tenant-id", List.of("t-c")),
            Map.of("tenant-id", List.of("t-n")),
            EXTEND,
            Map.of("tenant-id", List.of("t-c", "t-n"))),
        of(
            Map.of("tenant-id", List.of("t-c")),
            Map.of("tenant-id", List.of("t-n")),
            REPLACE,
            Map.of("tenant-id", List.of("t-n"))),
        of(
            Map.of("tenant-id", List.of("t-c")),
            Map.of("tenant-id", List.of("t-n")),
            KEEP,
            Map.of("tenant-id", List.of("t-c"))),
        of(
            Map.of("tenant-id", List.of("t-c"), "processes", List.of("p-c1", "p-c2")),
            Map.of("tenant-id", List.of(""), "processes", List.of("p-n3", "p-n4")),
            EXTEND,
            Map.of(
                "tenant-id", List.of("t-c"), "processes", List.of("p-c1", "p-c2", "p-n3", "p-n4"))),
        of(
            Map.of("tenant-id", List.of("t-c")),
            Map.of("processes", List.of("p-n")),
            EXTEND,
            Map.of("tenant-id", List.of("t-c"), "processes", List.of("p-n"))),
        of(Map.of("tenant-id", List.of("t-c")), null, EXTEND, Map.of("tenant-id", List.of("t-c"))),
        of(
            Map.of(),
            Map.of("tenant-id", List.of("t-n")),
            EXTEND,
            Map.of("tenant-id", List.of("t-n"))),
        of(
            Map.of("tenant-id", List.of("t-c")),
            Map.of("tenant-id", List.of("t-n"), "processes", List.of("p-n")),
            KEEP,
            Map.of("tenant-id", List.of("t-c"), "processes", List.of("p-n"))),
        of(
            Map.of("tenant-id", List.of("t-c"), "processes", List.of("p-c")),
            Map.of("tenant-id", List.of("t-n")),
            REPLACE,
            Map.of("tenant-id", List.of("t-n"), "processes", List.of("p-c"))));
  }
}
