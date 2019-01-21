package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.internal.connection.ServerAddressHelper;
import org.junit.ClassRule;
import org.junit.Test;

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
         .build();

   @Test()
   public void shouldStartMongoDbWithSpecifiedSettings() {
      try (MongoClient mongoClient = new MongoClient(ServerAddressHelper.createServerAddress(RULE.getHost()),
            MongoCredential.createCredential(DATABASE_USERNAME, DATABASE_NAME, DATABASE_PASSWORD.toCharArray()),
            MongoClientOptions.builder().build())) {
         assertThat(mongoClient.getCredential()).isNotNull();
         assertThat(mongoClient.getCredential().getUserName()).isEqualTo(DATABASE_USERNAME);
      }
   }
}