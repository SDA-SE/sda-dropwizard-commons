package org.sdase.commons.shared.asyncapi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonReferenceTest {

  @Test
  void shouldParseInternalRef() {
    JsonReference reference = JsonReference.parse("#/parse/internal");

    assertThat(reference.url).isNull();
    assertThat(reference.pointer).hasToString("/parse/internal");
  }

  @Test
  void shouldParseExternalRef() {
    JsonReference reference = JsonReference.parse("schema.json#/parse/external");

    assertThat(reference.url).isEqualTo("schema.json");
    assertThat(reference.pointer).hasToString("/parse/external");
  }

  @Test
  void shouldNotParseInvalidRef() {
    assertThatThrownBy(() -> JsonReference.parse("/parse/invalid"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldFormatInternalRefToString() {
    assertThat(JsonReference.parse("#/format/internal")).hasToString("#/format/internal");
  }

  @Test
  void shouldFormatExternalRefToString() {
    assertThat(JsonReference.parse("schema.json#/format/external"))
        .hasToString("schema.json#/format/external");
  }
}
