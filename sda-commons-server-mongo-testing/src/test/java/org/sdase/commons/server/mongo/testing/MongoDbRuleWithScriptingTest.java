package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClient;
import java.util.ArrayList;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Test;

public class MongoDbRuleWithScriptingTest {
  private static final String DATABASE_NAME = "my_db";
  private static final String DATABASE_USERNAME = "theuser";
  private static final String DATABASE_PASSWORD = "S3CR3t!"; // NOSONAR

  @ClassRule
  public static final MongoDbRule RULE =
      MongoDbRule.builder()
          .withDatabase(DATABASE_NAME)
          .withUsername(DATABASE_USERNAME)
          .withPassword(DATABASE_PASSWORD)
          .withTimeoutInMillis(30_000)
          .enableScripting()
          .build();

  @Test
  public void shouldSupportJavaScriptIfEnabled() {
    try (MongoClient mongoClient = RULE.createClient()) {
      ArrayList<Document> results =
          mongoClient
              .getDatabase("my_db")
              .getCollection("test")
              .find(new Document("$where", "this.name == 5"))
              .into(new ArrayList<Document>());
      assertThat(results).hasSize(0);
    }
  }
}
