package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

/** Tests if entities can be added by exact definition. */
public class MorphiaBundleEnsureIndexesIT {

  private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

  private static final LazyRule<DropwizardAppRule<Config>> DW =
      new LazyRule<>(
          () ->
              DropwizardRuleHelper.dropwizardTestAppFrom(MorphiaTestApp.class)
                  .withConfigFrom(Config::new)
                  .withRandomPorts()
                  .withConfigurationModifier(
                      c ->
                          c.getMongo()
                              .setHosts(MONGODB.getHost())
                              .setDatabase(MONGODB.getDatabase()))
                  .build());

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

  @Before
  public void verifyNoIndexBeforeAccessAndClean() {
    MONGODB.clearCollections();

    MongoCollection<Document> personCollection =
        getDatastore().getDatabase().getCollection("people");
    personCollection.dropIndexes();
    assertOnlyIndexesExist("_id_");
  }

  @Test
  public void ensureOnlyNewIndexes() {
    IndexEnsurer indexEnsurer = new IndexEnsurer(getDatastore(), false);

    indexEnsurer.ensureIndexes();

    assertOnlyIndexesExist("_id_", "name_1", "age_1");
  }

  @Test
  public void forceEnsureOnlyNewIndexes() {
    IndexEnsurer indexEnsurer = new IndexEnsurer(getDatastore(), true);

    indexEnsurer.ensureIndexes();

    assertOnlyIndexesExist("_id_", "name_1", "age_1");
  }

  @Test
  public void failOnIndexModification() {
    getDatastore()
        .getDatabase()
        .getCollection("people")
        .createIndex(
            new BsonDocument("age", new BsonInt32(1)),
            new IndexOptions().unique(true).name("age_1"));
    assertOnlyIndexesExist("_id_", "age_1");
    assertIndexUnique("age_1");
    IndexEnsurer indexEnsurer = new IndexEnsurer(getDatastore(), false);

    assertThatExceptionOfType(MongoCommandException.class)
        .isThrownBy(indexEnsurer::ensureIndexes)
        .withMessageContaining(" name: age_1 ");

    assertIndexUnique("age_1");
  }

  @Test
  public void forceIndexModification() {
    getDatastore()
        .getDatabase()
        .getCollection("people")
        .createIndex(
            new BsonDocument("age", new BsonInt32(1)),
            new IndexOptions().unique(true).name("age_1"));
    assertOnlyIndexesExist("_id_", "age_1");
    assertIndexUnique("age_1");
    IndexEnsurer indexEnsurer = new IndexEnsurer(getDatastore(), true);

    indexEnsurer.ensureIndexes();

    assertIndexNotUnique("age_1");
  }

  private Datastore getDatastore() {
    return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .forceEnsureIndexes()
            .build();

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build());
      bootstrap.addBundle(morphiaBundle);
    }

    @Override
    public void run(Config configuration, Environment environment) {
      // nothing to run
    }

    MorphiaBundle<Config> getMorphiaBundle() {
      return morphiaBundle;
    }
  }

  private void assertOnlyIndexesExist(String... indexNames) {
    Iterable<Document> indexInfo =
        getDatastore().getDatabase().getCollection("people").listIndexes();
    assertThat(indexInfo)
        .extracting(dbo -> dbo.get("name"))
        .containsExactlyInAnyOrder((Object[]) indexNames);
  }

  @SuppressWarnings("SameParameterValue")
  private void assertIndexUnique(String indexName) {
    assertThat(getDatastore().getDatabase().getCollection("people").listIndexes())
        .extracting(dbo -> dbo.get("name"), dbo -> dbo.get("unique"))
        .contains(tuple(indexName, true));
  }

  @SuppressWarnings("SameParameterValue")
  private void assertIndexNotUnique(String indexName) {
    assertThat(getDatastore().getDatabase().getCollection("people").listIndexes())
        .extracting(dbo -> dbo.get("name"), dbo -> dbo.get("unique"))
        .contains(tuple(indexName, null));
  }
}
