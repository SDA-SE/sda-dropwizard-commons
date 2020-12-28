package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.mongo.testing.MongoDbClassExtension.OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.testing.Environment;

class UseExistingMongoDbClassExtensionTest {

  @RegisterExtension
  static final MongoDb EXTERNAL_DB =
      MongoDbClassExtension.builder()
          .withDatabase("testDb")
          .withUsername("testuser")
          .withPassword("testpassword")
          .build();

  private MongoDbClassExtension useExistingMongoDbClassExtension;
  private String originalMongoDbConnectionString;

  @BeforeEach
  public void initUseExistingMongoDbExtension() {

    String MONGODB_CONNECTION_STRING =
        "mongodb://"
            + EXTERNAL_DB.getUsername()
            + ":"
            + EXTERNAL_DB.getPassword()
            + "@"
            + EXTERNAL_DB.getHosts()
            + "/"
            + EXTERNAL_DB.getDatabase()
            + "?"
            + EXTERNAL_DB.getOptions();
    originalMongoDbConnectionString =
        System.getenv().get(OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME);
    Environment.setEnv(OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME, MONGODB_CONNECTION_STRING);

    useExistingMongoDbClassExtension =
        MongoDbClassExtension.builder()
            .withDatabase("testDb")
            .withUsername("testuser")
            .withPassword("testpassword")
            .build();
    assertThat(useExistingMongoDbClassExtension)
        .isExactlyInstanceOf(UseExistingMongoDbClassExtension.class);

    MongoClient clientInTest = useExistingMongoDbClassExtension.createClient();
    clientInTest
        .getDatabase(useExistingMongoDbClassExtension.getDatabase())
        .getCollection("test")
        .insertOne(new Document("property", "example"));
  }

  @AfterEach
  public void clearDatabase() {
    useExistingMongoDbClassExtension.clearDatabase();
    Environment.unsetEnv(OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME);
    if (StringUtils.isNotBlank(originalMongoDbConnectionString)) {
      Environment.setEnv(
          OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME, originalMongoDbConnectionString);
    }
  }

  @Test
  void shouldWriteToExternalDb() {
    MongoClient externalDbClient = EXTERNAL_DB.createClient();
    FindIterable<Document> actualResult =
        externalDbClient.getDatabase(EXTERNAL_DB.getDatabase()).getCollection("test").find();
    assertThat(actualResult).hasSize(1);
    assertThat(actualResult.first()).extracting("property").isEqualTo("example");
  }
}
