package org.sdase.commons.server.morphia;

import com.mongodb.DBObject;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import xyz.morphia.Datastore;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.ModelMarker;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Tests if entities can be added by class path scanning from marker class.
 */
public class MorphiaBundleScanPackageByMarkerClassIT {

   private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

   private static final LazyRule<DropwizardAppRule<Config>> DW =
         new LazyRule<>(() ->
               DropwizardRuleHelper.dropwizardTestAppFrom(MorphiaTestApp.class)
                     .withConfigFrom(Config::new)
                     .withRandomPorts()
                     .withConfigurationModifier(c -> c.getMongo()
                           .setHosts(MONGODB.getHost())
                           .setDatabase("testPeople_" + new Random().nextInt(Integer.MAX_VALUE)))
                     .build());

   @ClassRule
   public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

   @Before
   public void verifyIndexBeforeAccess() {
      List<DBObject> indexInfo = getDatastore().getCollection(Person.class).getIndexInfo();
      assertThat(indexInfo).extracting(dbo -> dbo.get("name")).containsExactlyInAnyOrder("_id_", "name_1", "age_1");
   }

   @Test
   public void readAndWriteToMongoDb() {
      Datastore datastore = getDatastore();
      datastore.save(new Person().setName("John Doe").setAge(42));
      datastore.save(new Person().setName("Jane Doe").setAge(38));
      List<Person> people = datastore.find(Person.class).asList();
      assertThat(people)
            .extracting(Person::getName, Person::getAge)
            .containsExactly(
                  tuple("John Doe", 42),
                  tuple("Jane Doe", 38)
            );
   }

   private Datastore getDatastore() {
      return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().datastore();
   }

   public static class MorphiaTestApp extends Application<Config> {

      private MorphiaBundle<Config> morphiaBundle = MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntityScanPackageClass(ModelMarker.class)
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

}
