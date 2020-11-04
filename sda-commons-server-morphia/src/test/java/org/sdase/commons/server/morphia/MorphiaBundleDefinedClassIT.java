package org.sdase.commons.server.morphia;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.List;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;

/** Tests if entities can be added by exact definition. */
public class MorphiaBundleDefinedClassIT {

  private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          MorphiaTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mongo.hosts", MONGODB::getHost),
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
  public void readAndWriteToMongoDb() {
    Datastore datastore = getDatastore();
    datastore.save(new Person().setName("John Doe").setAge(42));
    datastore.save(new Person().setName("Jane Doe").setAge(38));
    List<Person> people = datastore.find(Person.class).find().toList();
    assertThat(people)
        .extracting(Person::getName, Person::getAge)
        .containsExactly(tuple("John Doe", 42), tuple("Jane Doe", 38));
  }

  private Datastore getDatastore() {
    return DW.<MorphiaTestApp>getApplication().getMorphiaBundle().datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
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
