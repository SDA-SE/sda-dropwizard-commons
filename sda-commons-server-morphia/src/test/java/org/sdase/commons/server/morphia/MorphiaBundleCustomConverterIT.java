package org.sdase.commons.server.morphia;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Key;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

/** Tests if entities can be added by exact definition. */
public class MorphiaBundleCustomConverterIT {

  private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          MorphiaTestApp.class,
          null,
          randomPorts(),
          config("mongo.hosts", MONGODB::getHosts),
          config("mongo.database", MONGODB::getDatabase));

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

  @Before
  public void verifyIndexBeforeAccessAndClean() {
    Iterable<Document> indexInfo =
        getDatastore().getDatabase().getCollection("people").listIndexes();
    assertThat(indexInfo)
        .extracting(dbo -> dbo.get("name"))
        .containsExactlyInAnyOrder("_id_", "name_1", "age_1");
    getDatastore().delete(getDatastore().find(Person.class));
  }

  @Test
  public void writeCustomPhoneNumber() {
    Datastore datastore = getDatastore();
    PhoneNumber phoneNumber =
        new PhoneNumber().setCountryCode("0049").setAreaCode("0172").setNumber("123 456 789");
    Key<Person> johnDoe =
        datastore.save(new Person().setName("John Doe").setAge(42).setPhoneNumber(phoneNumber));
    MongoClient mongoClient = DW.<MorphiaTestApp>getApplication().getMorphiaBundle().mongoClient();
    MongoDatabase database =
        mongoClient.getDatabase(DW.getConfiguration().getMongo().getDatabase());
    MongoCollection<Document> peopleCollection = database.getCollection("people");
    List<String> phoneNumbers = new ArrayList<>();
    peopleCollection
        .find()
        .forEach(
            (Consumer<? super Document>) d -> phoneNumbers.add(d.get("phoneNumber").toString()));
    assertThat(phoneNumbers).containsExactly("+49 172 123456789");

    Person johnDoeFromMorphia =
        datastore.find(Person.class).field("id").equal(johnDoe.getId()).first();
    assertThat(johnDoeFromMorphia).isNotNull();
    assertThat(johnDoeFromMorphia.getPhoneNumber())
        .extracting(PhoneNumber::getCountryCode, PhoneNumber::getAreaCode, PhoneNumber::getNumber)
        .containsExactly("+49", "172", "123456789");
  }

  private Datastore getDatastore() {
    return DW.<MorphiaTestApp>getApplication().getMorphiaBundle().datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
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
