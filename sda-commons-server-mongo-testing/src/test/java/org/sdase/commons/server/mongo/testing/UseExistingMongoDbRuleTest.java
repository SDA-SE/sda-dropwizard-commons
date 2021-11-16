package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.testing.SystemPropertyRule;

public class UseExistingMongoDbRuleTest {

  @ClassRule
  public static final MongoDbRule EXTERNAL_DB =
      MongoDbRule.builder()
          .withDatabase("testDb")
          .withUsername("testuser")
          .withPassword("testpassword")
          .build();

  private static SystemPropertyRule SYSTEM_PROPERTY_RULE;

  private MongoDbRule useExistingMongoDbRule;

  @BeforeClass
  public static void initEnvAfterStartOfExternalDb() {
    SYSTEM_PROPERTY_RULE =
        new SystemPropertyRule()
            .setProperty(
                "TEST_MONGODB_CONNECTION_STRING",
                "mongodb://"
                    + EXTERNAL_DB.getUsername()
                    + ":"
                    + EXTERNAL_DB.getPassword()
                    + "@"
                    + EXTERNAL_DB.getHosts()
                    + "/"
                    + EXTERNAL_DB.getDatabase()
                    + "?"
                    + EXTERNAL_DB.getOptions());
  }

  @Before
  public void initUseExistingMongoDbRule() throws Throwable {
    SYSTEM_PROPERTY_RULE
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                useExistingMongoDbRule = MongoDbRule.builder().build();
              }
            },
            Description.EMPTY)
        .evaluate();
    assertThat(useExistingMongoDbRule).isExactlyInstanceOf(UseExistingMongoDbRule.class);
  }

  @After
  public void clearDatabase() {
    useExistingMongoDbRule.clearDatabase();
  }

  @Test
  public void shouldWriteToExternalDb() throws Throwable {
    useExistingMongoDbRule
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                MongoClient clientInTest = useExistingMongoDbRule.createClient();
                clientInTest
                    .getDatabase(useExistingMongoDbRule.getDatabase())
                    .getCollection("test")
                    .insertOne(new Document("property", "example"));
              }
            },
            Description.EMPTY)
        .evaluate();

    MongoClient externalDbClient = EXTERNAL_DB.createClient();
    FindIterable<Document> actualResult =
        externalDbClient.getDatabase(EXTERNAL_DB.getDatabase()).getCollection("test").find();
    assertThat(actualResult).hasSize(1);
    assertThat(actualResult.first()).extracting("property").isEqualTo("example");
  }
}
