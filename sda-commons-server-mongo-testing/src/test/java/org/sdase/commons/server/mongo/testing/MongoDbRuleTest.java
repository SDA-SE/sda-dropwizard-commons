package org.sdase.commons.server.mongo.testing;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.internal.connection.ServerAddressHelper;
import de.flapdoodle.embed.mongo.distribution.Version;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoDbRuleTest {
   private static final String DATABASE_NAME = "my_db";
   private static final String DATABASE_USERNAME = "theuser";
   private static final String DATABASE_PASSWORD = "S3CR3t!"; // NOSONAR

   @ClassRule
   public static final MongoDbRule RULE = MongoDbRule
         .builder()
         .withDatabase(DATABASE_NAME)
         .withUsername(DATABASE_USERNAME)
         .withPassword(DATABASE_PASSWORD)
         .withTimeoutInMillis(30_000)
         .withVersion(Version.V3_6_5)
         .build();

   @Test
   public void shouldStartMongoDbWithSpecifiedSettings() {
      try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
         assertThat(mongoClient.getCredential()).isNotNull();
         assertThat(mongoClient.getCredential().getUserName()).isEqualTo(DATABASE_USERNAME);
         long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
         assertThat(documentCount).isEqualTo(0);
      }
   }

   @Test(expected = MongoSecurityException.class)
   public void shouldRejectAccessForBadCredentials() {
      try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(DATABASE_USERNAME, DATABASE_NAME, (DATABASE_PASSWORD + "_bad").toCharArray()),
            MongoClientOptions.builder().build())) {
         mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
      }
   }

   @Test // Flapdoodle can not require auth and create a user
   public void shouldAllowAccessWithoutCredentials() {
      try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoClientOptions.builder().build())) {
         long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
         assertThat(documentCount).isEqualTo(0);
      }
   }

   @Test
   public void shouldProvideClientForTesting() {
      try (MongoClient mongoClient = RULE.createClient()) {
         long documentCount = mongoClient.getDatabase("my_db").getCollection("test").countDocuments();
         assertThat(documentCount).isEqualTo(0);
      }
   }

   @Test
   public void shouldClearCollections() {
      try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
         MongoDatabase db = mongoClient.getDatabase("my_db");
         MongoCollection<Document> collection = db.getCollection("clearCollectionsTest");
         collection.createIndex(Indexes.ascending("field"));
         collection.insertOne(new Document().append("field", "value"));

         RULE.clearCollections();

         assertThat(db.listCollectionNames()).contains("clearCollectionsTest");
         assertThat(collection.listIndexes()).isNotEmpty();
         assertThat(collection.countDocuments()).isEqualTo(0);
      }
   }

   @Test
   public void shouldClearDatabase() {
      try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
         MongoDatabase db = mongoClient.getDatabase("my_db");
         db.getCollection("clearDatabaseTest").insertOne(new Document().append("Hallo", "Welt"));

         RULE.clearDatabase();

         assertThat(db.listCollectionNames()).doesNotContain("clearDatabaseTest");
      }
   }
}