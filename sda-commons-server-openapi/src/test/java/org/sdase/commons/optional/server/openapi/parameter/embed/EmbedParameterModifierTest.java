package org.sdase.commons.optional.server.openapi.parameter.embed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmbedParameterModifierTest {

  @Test
  void shouldReturnNullForNullSchemas() {
    assertThat(new EmbedParameterModifier().getOriginalRef(null)).isNull();
  }
}
