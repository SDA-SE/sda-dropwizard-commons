package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class MongoDbClassExtensionBuilderTest {

  @Test
  void shouldUseDefaultValues() {
    assertThatCode(
            () -> {
              final MongoDbClassExtension extension = MongoDbClassExtension.builder().build();
              assertThat(extension.getUsername()).isNotNull();
              assertThat(extension.getPassword()).isNotNull();
              assertThat(extension.getDatabase()).isNotNull();
            })
        .doesNotThrowAnyException();
  }
}
