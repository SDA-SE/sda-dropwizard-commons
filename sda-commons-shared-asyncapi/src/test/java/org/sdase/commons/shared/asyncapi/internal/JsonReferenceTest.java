package org.sdase.commons.shared.asyncapi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class JsonReferenceTest {

  @Test
  public void shouldParseInternalRef() {
    JsonReference reference = JsonReference.parse("#/parse/internal");

    assertThat(reference.url).isNull();
    assertThat(reference.pointer.toString()).isEqualTo("/parse/internal");
  }

  @Test
  public void shouldParseExternalRef() {
    JsonReference reference = JsonReference.parse("schema.json#/parse/external");

    assertThat(reference.url).isEqualTo("schema.json");
    assertThat(reference.pointer.toString()).isEqualTo("/parse/external");
  }

  @Test
  public void shouldNotParseInvalidRef() {
    assertThatThrownBy(() -> JsonReference.parse("/parse/invalid"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldFormatInternalRefToString() {
    assertThat(JsonReference.parse("#/format/internal").toString()).isEqualTo("#/format/internal");
  }

  @Test
  public void shouldFormatExternalRefToString() {
    assertThat(JsonReference.parse("schema.json#/format/external").toString())
        .isEqualTo("schema.json#/format/external");
  }
}
