package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClient;
import java.util.ArrayList;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class MongoDbClassExtensionWithScriptingTest {
  private static final String DATABASE_NAME = "my_db";
  private static final String DATABASE_USERNAME = "theuser";
  private static final String DATABASE_PASSWORD = "S3CR3t!"; // NOSONAR

  @RegisterExtension
  static final MongoDbClassExtension MONGO_DB_EXTENSION =
      MongoDbClassExtension.builder()
          .withDatabase(DATABASE_NAME)
          .withUsername(DATABASE_USERNAME)
          .withPassword(DATABASE_PASSWORD)
          .withTimeoutInMillis(30_000)
          .enableScripting()
          .build();

  @Test
  void shouldSupportJavaScriptIfEnabled() {
    try (MongoClient mongoClient = MONGO_DB_EXTENSION.createClient()) {
      ArrayList<Document> results =
          mongoClient
              .getDatabase("my_db")
              .getCollection("test")
              .find(new Document("$where", "this.name == 5"))
              .into(new ArrayList<Document>());
      assertThat(results).isEmpty();
    }
  }
}
