package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.internal.connection.ServerAddressHelper;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.util.ArrayList;
import org.apache.commons.lang3.SystemUtils;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MongoDbExtensionTest {
  private static final String DATABASE_NAME = "my_db";
  private static final String DATABASE_USERNAME = "theuser";
  private static final String DATABASE_PASSWORD = "S3CR3t!"; // NOSONAR

  @RegisterExtension
  static final MongoDbExtension MONGO_DB_EXTENSION =
      MongoDbExtension.builder()
          .withDatabase(DATABASE_NAME)
          .withUsername(DATABASE_USERNAME)
          .withPassword(DATABASE_PASSWORD)
          .withTimeoutInMillis(30_000)
          .build();

  @Test
  void shouldStartMongoDbWithSpecifiedSettings() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
      assertThat(mongoClient.getCredential()).isNotNull();
      assertThat(mongoClient.getCredential().getUserName()).isEqualTo(DATABASE_USERNAME);
      long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      assertThat(documentCount).isZero();
    }
  }

  @Test
  void shouldRejectAccessForBadCredentials() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, (DATABASE_PASSWORD + "_bad").toCharArray()),
            MongoClientOptions.builder().build())) {
      assertThatThrownBy(
              () -> mongoClient.getDatabase("my_db").getCollection("test").countDocuments())
          .isInstanceOf(MongoSecurityException.class);
    }
  }

  @Test // Flapdoodle can not require auth and create a user
  void shouldAllowAccessWithoutCredentials() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHost()),
            MongoClientOptions.builder().build())) {
      long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      assertThat(documentCount).isZero();
    }
  }

  @Test
  void shouldProvideClientForTesting() {
    try (MongoClient mongoClient = MONGO_DB_EXTENSION.createClient()) {
      long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      assertThat(documentCount).isZero();
    }
  }

  @Test
  void shouldNotSupportJavaScriptByDefault() {
    try (MongoClient mongoClient = MONGO_DB_EXTENSION.createClient()) {
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
  void shouldClearCollections() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
      MongoDatabase db = mongoClient.getDatabase("my_db");
      MongoCollection<Document> collection = db.getCollection("clearCollectionsTest");
      collection.createIndex(Indexes.ascending("field"));
      collection.insertOne(new Document().append("field", "value"));

      MONGO_DB_EXTENSION.clearCollections();

      assertThat(db.listCollectionNames()).contains("clearCollectionsTest");
      assertThat(collection.listIndexes()).isNotEmpty();
      assertThat(collection.countDocuments()).isZero();
    }
  }

  @Test
  void shouldClearDatabase() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHost()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
      MongoDatabase db = mongoClient.getDatabase("my_db");
      db.getCollection("clearDatabaseTest").insertOne(new Document().append("Hallo", "Welt"));

      MONGO_DB_EXTENSION.clearDatabase();

      assertThat(db.listCollectionNames()).doesNotContain("clearDatabaseTest");
    }
  }

  @Test
  void shouldTakeSpecificMongoDbVersion() {
    final IFeatureAwareVersion specificMongoDbVersion = Version.V3_2_20;
    assertThat(MongoDbExtension.builder().withVersion(specificMongoDbVersion).build().getVersion())
        .isEqualTo(specificMongoDbVersion);
  }

  @Test
  void shouldDetermineMongoDbVersionIfVersionIsNull() {
    assertThat(MongoDbExtension.builder().withVersion(null).build().getVersion())
        .isIn(MongoDbExtension.Builder.DEFAULT_VERSION, MongoDbExtension.Builder.WINDOWS_VERSION);
  }

  @Test
  void shouldUseOsSpecificMongoDbVersion() {
    final IFeatureAwareVersion mongoDbVersion = MongoDbExtension.builder().build().getVersion();
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(mongoDbVersion).isEqualTo(MongoDbExtension.Builder.WINDOWS_VERSION);
      assertThat(mongoDbVersion).isNotEqualTo(MongoDbExtension.Builder.DEFAULT_VERSION);
    } else {
      assertThat(mongoDbVersion).isEqualTo(MongoDbExtension.Builder.DEFAULT_VERSION);
      assertThat(mongoDbVersion).isNotEqualTo(MongoDbExtension.Builder.WINDOWS_VERSION);
    }
  }
}
