package org.sdase.commons.server.morphia;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.morphia.test.model.PhoneNumber;
import org.sdase.commons.server.morphia.test.model.PhoneNumberConverter;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;
import xyz.morphia.Datastore;
import xyz.morphia.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if entities can be added by exact definition.
 */
public class MorphiaBundleCustomConverterIT {

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
   public void verifyIndexBeforeAccessAndClean() {
      List<DBObject> indexInfo = getDatastore().getCollection(Person.class).getIndexInfo();
      assertThat(indexInfo).extracting(dbo -> dbo.get("name")).containsExactlyInAnyOrder("_id_", "name_1", "age_1");
      getDatastore().delete(getDatastore().find(Person.class));
   }

   @Test
   public void writeCustomPhoneNumber() {
      Datastore datastore = getDatastore();
      PhoneNumber phoneNumber = new PhoneNumber()
            .setCountryCode("0049")
            .setAreaCode("0172")
            .setNumber("123 456 789");
      Key<Person> johnDoe = datastore.save(new Person().setName("John Doe").setAge(42).setPhoneNumber(phoneNumber));
      MongoClient mongoClient = ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().mongoClient();
      MongoDatabase database = mongoClient.getDatabase(DW.getRule().getConfiguration().getMongo().getDatabase());
      MongoCollection<Document> peopleCollection = database.getCollection("people");
      List<String> phoneNumbers = new ArrayList<>();
      peopleCollection.find().forEach((Consumer<? super Document>) d -> phoneNumbers.add(d.get("phoneNumber").toString()));
      assertThat(phoneNumbers).containsExactly("+49 172 123456789");

      Person johnDoeFromMorphia = datastore.find(Person.class).field("id").equal(johnDoe.getId()).get();
      assertThat(johnDoeFromMorphia.getPhoneNumber())
            .extracting(PhoneNumber::getCountryCode, PhoneNumber::getAreaCode, PhoneNumber::getNumber)
            .containsExactly("+49", "172", "123456789");
   }

   private Datastore getDatastore() {
      return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().datastore();
   }

   public static class MorphiaTestApp extends Application<Config> {

      private MorphiaBundle<Config> morphiaBundle = MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .disableDefaultTypeConverters()
            .addCustomTypeConverter(new PhoneNumberConverter())
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
