package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MongoDbClassExtensionBuilderTest {

  @RegisterExtension
  static final MongoDbClassExtension MONGO = MongoDbClassExtension.builder().build();

  @Test
  void shouldUseDefaultValues() {
    assertThat(MONGO.getMongoConnectionString()).isNotNull();
    assertThat(MONGO.getMongoConnectionString().getUsername()).isNotNull();
    assertThat(MONGO.getMongoConnectionString().getPassword()).isNotNull();
    assertThat(MONGO.getMongoConnectionString().getDatabase()).isNotNull();
  }
}
