package org.sdase.commons.server.kafka.consumer.strategies;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CaseInsensitiveHeaderTest {

  @ParameterizedTest
  @ValueSource(strings = {"tenant-id", "Tenant-Id", "tenant-ID"})
  void shouldMapToSameKey(String originalKey) {
    // given
    var ignoredValue = "ignored";
    var expected = "tenant-id";
    var header =
        new CaseInsensitiveHeader(
            new RecordHeader(originalKey, ignoredValue.getBytes(StandardCharsets.UTF_8)));
    assertThat(header)
        .extracting(CaseInsensitiveHeader::key, v -> new String(v.value()))
        .containsExactly(expected, ignoredValue);
  }

  @Test
  void shouldThrowException() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new CaseInsensitiveHeader(null))
        .withMessage("Header can not be null.");
  }

  @Test
  void shouldMapToNullKey() {
    var header =
        new CaseInsensitiveHeader(
            new RecordHeader("", "ignoredValue".getBytes(StandardCharsets.UTF_8)));
    assertThat(header).extracting(CaseInsensitiveHeader::key).isNull();
  }
}
