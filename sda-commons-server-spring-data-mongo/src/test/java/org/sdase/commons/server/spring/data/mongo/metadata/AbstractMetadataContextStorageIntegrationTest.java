package org.sdase.commons.server.spring.data.mongo.metadata;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.distribution.Version;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.metadata.test.MetadataCompatibilityTestApp;
import org.sdase.commons.server.spring.data.mongo.metadata.test.MetadataTestApp;
import org.sdase.commons.server.spring.data.mongo.metadata.test.MetadataTestAppConfig;
import org.sdase.commons.server.spring.data.mongo.metadata.test.model.BusinessEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoOperations;

abstract class AbstractMetadataContextStorageIntegrationTest {

  static final Logger LOG =
      LoggerFactory.getLogger(AbstractMetadataContextStorageIntegrationTest.class);

  static final class DefaultSpringDataMongoConfigTestMongo44
      extends AbstractMetadataContextStorageIntegrationTest {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension MONGO =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MetadataTestAppConfig> DW =
        new DropwizardAppExtension<>(
            MetadataTestApp.class,
            null,
            config("mongo.connectionString", MONGO::getConnectionString));

    @Override
    MongoOperations getMongoOperations() {
      return ((MetadataTestApp) DW.getApplication()).getMongoOperations();
    }

    @Override
    MongoDbClassExtension getMongo() {
      return MONGO;
    }
  }

  static final class DefaultSpringDataMongoConfigTestMongo50
      extends AbstractMetadataContextStorageIntegrationTest {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension MONGO =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MetadataTestAppConfig> DW =
        new DropwizardAppExtension<>(
            MetadataTestApp.class,
            null,
            config("mongo.connectionString", MONGO::getConnectionString));

    @Override
    MongoOperations getMongoOperations() {
      return ((MetadataTestApp) DW.getApplication()).getMongoOperations();
    }

    @Override
    MongoDbClassExtension getMongo() {
      return MONGO;
    }
  }

  static final class MorphiaCompatibilityConfigTestMongo44
      extends AbstractMetadataContextStorageIntegrationTest {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension MONGO =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MetadataTestAppConfig> DW =
        new DropwizardAppExtension<>(
            MetadataCompatibilityTestApp.class,
            null,
            config("mongo.connectionString", MONGO::getConnectionString));

    @Override
    MongoOperations getMongoOperations() {
      return ((MetadataCompatibilityTestApp) DW.getApplication()).getMongoOperations();
    }

    @Override
    MongoDbClassExtension getMongo() {
      return MONGO;
    }
  }

  static final class MorphiaCompatibilityConfigTestMongo50
      extends AbstractMetadataContextStorageIntegrationTest {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension MONGO =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MetadataTestAppConfig> DW =
        new DropwizardAppExtension<>(
            MetadataCompatibilityTestApp.class,
            null,
            config("mongo.connectionString", MONGO::getConnectionString));

    @Override
    MongoOperations getMongoOperations() {
      return ((MetadataCompatibilityTestApp) DW.getApplication()).getMongoOperations();
    }

    @Override
    MongoDbClassExtension getMongo() {
      return MONGO;
    }
  }

  MongoOperations mongoOperations;

  abstract MongoOperations getMongoOperations();

  abstract MongoDbClassExtension getMongo();

  @BeforeEach
  void setUp() {
    getMongo().clearCollections();
    mongoOperations = getMongoOperations();
  }

  @Test
  void shouldSaveMetadataContext() {
    var givenContext = new DetachedMetadataContext();
    givenContext.put("tenant-id", List.of("tenant-1"));
    givenContext.put("processes", List.of("p-1", "p-2"));
    var givenEntity =
        new BusinessEntity().setId(UUID.randomUUID().toString()).setMetadata(givenContext);

    mongoOperations.insert(givenEntity);

    try (var client = getMongo().createClient()) {
      var collection = businessEntityCollection(client);
      var doc = collection.find().first();

      LOG.info("Stored: {}", doc);

      assertThat(doc)
          .isNotNull()
          .extracting("metadata")
          .isNotNull()
          .asInstanceOf(InstanceOfAssertFactories.MAP)
          .containsExactly(
              entry("tenant-id", List.of("tenant-1")), entry("processes", List.of("p-1", "p-2")));
    }
  }

  @Test
  void shouldLoadMetadataContextWithoutClassInfo() {

    var given =
        Document.parse(
            "{"
                + "\"_id\": \"038EBE14-37FB-4531-BA83-CE52B38D5EB0\","
                + "\"metadata\": {"
                + "\"tenant-id\": [\"tenant1\"],"
                + "\"processes\": [\"p1\",\"p2\"]"
                + "}"
                + "}");
    try (var client = getMongo().createClient()) {
      businessEntityCollection(client).insertOne(given);
    }

    var actual = mongoOperations.findAll(BusinessEntity.class).stream().findFirst().orElse(null);

    LOG.info("Loaded: {}", actual);

    assertThat(actual)
        .isNotNull()
        .extracting(BusinessEntity::getMetadata)
        .isNotNull()
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsExactly(
            entry("tenant-id", List.of("tenant1")), entry("processes", List.of("p1", "p2")));
  }

  @Test
  void shouldLoadMetadataContextWithClassInfo() {

    var given =
        Document.parse(
            "{"
                + "\"_id\": \"038EBE14-37FB-4531-BA83-CE52B38D5EB0\","
                + "\"_class\": \"org.sdase.commons.server.spring.data.mongo.metadata.test.model.BusinessEntity\","
                + "\"metadata\": {"
                + "\"tenant-id\": [\"tenant1\"],"
                + "\"processes\": [\"p1\",\"p2\"]"
                + "}"
                + "}");
    try (var client = getMongo().createClient()) {
      businessEntityCollection(client).insertOne(given);
    }

    var actual = mongoOperations.findAll(BusinessEntity.class).stream().findFirst().orElse(null);

    LOG.info("Loaded: {}", actual);

    assertThat(actual)
        .isNotNull()
        .extracting(BusinessEntity::getMetadata)
        .isNotNull()
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsExactly(
            entry("tenant-id", List.of("tenant1")), entry("processes", List.of("p1", "p2")));
  }

  @Test
  void shouldLoadMetadataContextWithCompatibilityClassInfo() {

    var given =
        Document.parse(
            "{"
                + "\"_id\": \"038EBE14-37FB-4531-BA83-CE52B38D5EB0\","
                + "\"className\": \"org.sdase.commons.server.spring.data.mongo.metadata.test.model.BusinessEntity\","
                + "\"metadata\": {"
                + "\"tenant-id\": [\"tenant1\"],"
                + "\"processes\": [\"p1\",\"p2\"]"
                + "}"
                + "}");
    try (var client = getMongo().createClient()) {
      businessEntityCollection(client).insertOne(given);
    }

    var actual = mongoOperations.findAll(BusinessEntity.class).stream().findFirst().orElse(null);

    LOG.info("Loaded: {}", actual);

    assertThat(actual)
        .isNotNull()
        .extracting(BusinessEntity::getMetadata)
        .isNotNull()
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsExactly(
            entry("tenant-id", List.of("tenant1")), entry("processes", List.of("p1", "p2")));
  }

  @Test
  void shouldLoadSavedMetadataContext() {
    var givenContext = new DetachedMetadataContext();
    givenContext.put("tenant-id", List.of("tenant-1"));
    givenContext.put("processes", List.of("p-1", "p-2"));
    var givenEntity =
        new BusinessEntity().setId(UUID.randomUUID().toString()).setMetadata(givenContext);

    mongoOperations.insert(givenEntity);

    var actual = mongoOperations.findAll(BusinessEntity.class).stream().findFirst().orElse(null);

    LOG.info("Loaded: {}", actual);

    assertThat(actual)
        .isNotNull()
        .extracting(BusinessEntity::getMetadata)
        .isNotNull()
        .asInstanceOf(InstanceOfAssertFactories.MAP)
        .containsExactly(
            entry("tenant-id", List.of("tenant-1")), entry("processes", List.of("p-1", "p-2")));
  }

  private MongoCollection<Document> businessEntityCollection(MongoClient client) {
    return client.getDatabase(getMongo().getDatabase()).getCollection("businessEntity");
  }
}
