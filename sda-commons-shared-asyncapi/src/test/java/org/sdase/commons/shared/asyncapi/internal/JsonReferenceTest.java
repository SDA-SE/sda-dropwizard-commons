package org.sdase.commons.shared.asyncapi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class JsonReferenceTest {

  @Test
  public void shouldParseInternalRef() {
    JsonReference reference = JsonReference.parse("#/parse/internal");

    assertThat(reference.url).isNull();
    assertThat(reference.pointer).hasToString("/parse/internal");
  }

  @Test
  public void shouldParseExternalRef() {
    JsonReference reference = JsonReference.parse("schema.json#/parse/external");

    assertThat(reference.url).isEqualTo("schema.json");
    assertThat(reference.pointer).hasToString("/parse/external");
  }

  @Test
  public void shouldNotParseInvalidRef() {
    assertThatThrownBy(() -> JsonReference.parse("/parse/invalid"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldFormatInternalRefToString() {
    assertThat(JsonReference.parse("#/format/internal")).hasToString("#/format/internal");
  }

  @Test
  public void shouldFormatExternalRefToString() {
    assertThat(JsonReference.parse("schema.json#/format/external"))
        .hasToString("schema.json#/format/external");
  }
}
