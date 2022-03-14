package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.internal.connection.ServerAddressHelper;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StartLocalMongoDbClassExtensionTest {
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
          .withVersion(Version.V3_2_20)
          .build();

  @Test
  void shouldStartMongoDbWithSpecifiedSettings() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHosts()),
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
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHosts()),
            MongoCredential.createCredential(
                DATABASE_USERNAME, DATABASE_NAME, (DATABASE_PASSWORD + "_bad").toCharArray()),
            MongoClientOptions.builder().build())) {
      MongoCollection<Document> collection = mongoClient.getDatabase("my_db").getCollection("test");

      assertThatThrownBy(collection::countDocuments).isInstanceOf(MongoSecurityException.class);
    }
  }

  @Test
  // Flapdoodle can not require auth and create a user
  void shouldAllowAccessWithoutCredentials() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHosts()),
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
      FindIterable<Document> documents =
          mongoClient
              .getDatabase("my_db")
              .getCollection("test")
              .find(new Document("$where", "this.name == 5"));
      List<Document> list = new ArrayList<>();

      assertThatThrownBy(() -> documents.into(list))
          .isInstanceOf(MongoQueryException.class)
          .hasMessageContaining("no globalScriptEngine in $where parsing");
    }
  }

  @Test
  void shouldClearCollections() {
    try (MongoClient mongoClient =
        new MongoClient(
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHosts()),
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
            ServerAddressHelper.createServerAddress(MONGO_DB_EXTENSION.getHosts()),
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
    assertThat(MONGO_DB_EXTENSION.getServerVersion()).isEqualTo("3.2.20");
  }

  @Test
  void shouldStartSpecificMongoDbVersion() {
    assertThat(MONGO_DB_EXTENSION.getServerVersion()).isEqualTo("3.2.20");
  }

  @Test
  void shouldDetermineMongoDbVersionIfVersionIsNull() {
    MongoDbClassExtension mongoDbClassExtension =
        MongoDbClassExtension.builder()
            .withDatabase("test")
            .withPassword("test")
            .withUsername("test")
            .build();
    assertThat(mongoDbClassExtension)
        .extracting("version")
        .isIn(MongoDbRule.Builder.DEFAULT_VERSION, MongoDbRule.Builder.WINDOWS_VERSION);
  }

  @Test
  void shouldUseOsSpecificMongoDbVersion() {
    MongoDbClassExtension mongoDbClassExtension =
        MongoDbClassExtension.builder()
            .withDatabase("test")
            .withPassword("test")
            .withUsername("test")
            .build();
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(mongoDbClassExtension)
          .extracting("version")
          .isEqualTo(MongoDbClassExtension.Builder.WINDOWS_VERSION);
    } else {
      assertThat(mongoDbClassExtension)
          .extracting("version")
          .isEqualTo(MongoDbClassExtension.Builder.DEFAULT_VERSION);
    }
  }
}
