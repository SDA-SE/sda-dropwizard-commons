package org.sdase.commons.server.morphia;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.client.model.IndexOptions;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;
import xyz.morphia.Datastore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests if entities can be added by exact definition.
 */
public class MorphiaBundleEnsureIndexesIT {

   private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

   private static final LazyRule<DropwizardAppRule<Config>> DW =
         new LazyRule<>(() ->
               DropwizardRuleHelper.dropwizardTestAppFrom(MorphiaTestApp.class)
                     .withConfigFrom(Config::new)
                     .withRandomPorts()
                     .withConfigurationModifier(c -> c.getMongo()
                           .setHosts(MONGODB.getHost())
                           .setDatabase("testPeople"))
                     .build());

   @ClassRule
   public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

   @Before
   public void verifyNoIndexBeforeAccessAndClean() {
      MONGODB.clearCollections();

      DBCollection personCollection = getDatastore().getCollection(Person.class);
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
      getDatastore().getMongo().getDatabase("testPeople").getCollection("people")
            .createIndex(
                  new BsonDocument("age", new BsonInt32(1)),
                  new IndexOptions()
                        .unique(true)
                        .name("age_1"));
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
      getDatastore().getMongo().getDatabase("testPeople").getCollection("people")
            .createIndex(
                  new BsonDocument("age", new BsonInt32(1)),
                  new IndexOptions()
                        .unique(true)
                        .name("age_1"));
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

      private MorphiaBundle<Config> morphiaBundle = MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .forceEnsureIndexes()
            .build();

      @Override
      public void initialize(Bootstrap<Config> bootstrap) {
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
      List<DBObject> indexInfo = getDatastore().getCollection(Person.class).getIndexInfo();
      assertThat(indexInfo).extracting(dbo -> dbo.get("name")).containsExactlyInAnyOrder(indexNames);
   }

   @SuppressWarnings("SameParameterValue")
   private void assertIndexUnique(String indexName) {
      assertThat(getDatastore().getCollection(Person.class).getIndexInfo())
            .extracting(dbo -> dbo.get("name"), dbo -> dbo.get("unique"))
            .contains(tuple(indexName, true));
   }

   @SuppressWarnings("SameParameterValue")
   private void assertIndexNotUnique(String indexName) {
      assertThat(getDatastore().getCollection(Person.class).getIndexInfo())
            .extracting(dbo -> dbo.get("name"), dbo -> dbo.get("unique"))
            .contains(tuple(indexName, null));
   }
}
