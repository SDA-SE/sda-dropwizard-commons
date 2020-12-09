package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoQueryException;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.internal.connection.ServerAddressHelper;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.util.ArrayList;
import org.apache.commons.lang3.SystemUtils;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MongoDbRuleTest {
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
          .build();

  @Test
  public void shouldStartMongoDbWithSpecifiedSettings() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
      assertThat(mongoClient.getCredential()).isNotNull();
      assertThat(mongoClient.getCredential().getUserName()).isEqualTo(DATABASE_USERNAME);
      long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      assertThat(documentCount).isZero();
    }
  }

  @Test(expected = MongoSecurityException.class)
  public void shouldRejectAccessForBadCredentials() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, (DATABASE_PASSWORD + "_bad").toCharArray()),
            MongoClientOptions.builder().build())) {
      mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
    }
  }

  @Test // Flapdoodle can not require auth and create a user
  public void shouldAllowAccessWithoutCredentials() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoClientOptions.builder().build())) {
      long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      assertThat(documentCount).isZero();
    }
  }

  @Test
  public void shouldProvideClientForTesting() {
    try (MongoClient mongoClient = RULE.createClient()) {
      long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      assertThat(documentCount).isZero();
    }
  }

  @Test
  public void shouldNotSupportJavaScriptByDefault() {
    try (MongoClient mongoClient = RULE.createClient()) {
      assertThatThrownBy(
              () ->
                  mongoClient
                      .getDatabase("my_db")
                      .getCollection("test")
                      .find(new Document("$where", "this.name == 5"))
                      .into(new ArrayList<Document>()))
          .isInstanceOf(MongoQueryException.class)
          .hasMessageContaining("no globalScriptEngine in $where parsing");
    }
  }

  @Test
  public void shouldClearCollections() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
      MongoDatabase db = mongoClient.getDatabase("my_db");
      MongoCollection<Document> collection = db.getCollection("clearCollectionsTest");
      collection.createIndex(Indexes.ascending("field"));
      collection.insertOne(new Document().append("field", "value"));

      RULE.clearCollections();

      assertThat(db.listCollectionNames()).contains("clearCollectionsTest");
      assertThat(collection.listIndexes()).isNotEmpty();
      assertThat(collection.countDocuments()).isZero();
    }
  }

  @Test
  public void shouldClearDatabase() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
      MongoDatabase db = mongoClient.getDatabase("my_db");
      db.getCollection("clearDatabaseTest").insertOne(new Document().append("Hallo", "Welt"));

      RULE.clearDatabase();

      assertThat(db.listCollectionNames()).doesNotContain("clearDatabaseTest");
    }
  }

  @Test
  public void shouldTakeSpecificMongoDbVersion() {
    IFeatureAwareVersion specificMongoDbVersion = Version.V3_2_20;
    MongoDbRule mongoDbRule = MongoDbRule.builder().withVersion(specificMongoDbVersion).build();
    assumeThat(mongoDbRule).isExactlyInstanceOf(StartLocalMongoDbRule.class);
    assertThat(mongoDbRule).extracting("version").isEqualTo(specificMongoDbVersion);
  }

  @Test
  public void shouldStartSpecificMongoDbVersion() {
    final IFeatureAwareVersion specificMongoDbVersion = Version.V3_2_20;
    MongoDbRule mongoDbRule = MongoDbRule.builder().withVersion(specificMongoDbVersion).build();
    mongoDbRule.apply(
        new Statement() {
          @Override
          public void evaluate() {
            assertThat(mongoDbRule.getServerVersion()).isEqualTo("3.2.20");
          }
        },
        Description.EMPTY);
  }

  @Test
  public void shouldDetermineMongoDbVersionIfVersionIsNull() {
    final MongoDbRule mongoDbRule = MongoDbRule.builder().withVersion(null).build();
    assumeThat(mongoDbRule).isExactlyInstanceOf(StartLocalMongoDbRule.class);
    assertThat(mongoDbRule)
        .extracting("version")
        .isIn(MongoDbRule.Builder.DEFAULT_VERSION, MongoDbRule.Builder.WINDOWS_VERSION);
  }

  @Test
  public void shouldUseOsSpecificMongoDbVersion() {
    MongoDbRule mongoDbRule = MongoDbRule.builder().build();
    assumeThat(mongoDbRule).isExactlyInstanceOf(StartLocalMongoDbRule.class);
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(mongoDbRule).extracting("version").isEqualTo(MongoDbRule.Builder.WINDOWS_VERSION);
      assertThat(mongoDbRule)
          .extracting("version")
          .isNotEqualTo(MongoDbRule.Builder.DEFAULT_VERSION);
    } else {
      assertThat(mongoDbRule).extracting("version").isEqualTo(MongoDbRule.Builder.DEFAULT_VERSION);
      assertThat(mongoDbRule)
          .extracting("version")
          .isNotEqualTo(MongoDbRule.Builder.WINDOWS_VERSION);
    }
  }
}
