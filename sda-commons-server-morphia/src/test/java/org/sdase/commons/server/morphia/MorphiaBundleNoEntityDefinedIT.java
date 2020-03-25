package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Iterator;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

/** Tests if entities can be added if they are not in the scanned package. */
public class MorphiaBundleNoEntityDefinedIT {

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
  public void verifyIndexBeforeAccess() {
    Iterable<Document> indexInfo =
        getDatastore().getDatabase().getCollection("people").listIndexes();
    // No index is created if the entity is not found on scanning, although db operations seem to
    // work
    assertThat(indexInfo).isEmpty();
  }

  @Test
  public void readAndWriteToMongoDb() {
    Datastore datastore = getDatastore();
    datastore.save(new Person().setName("John Doe").setAge(42));
    datastore.save(new Person().setName("Jane Doe").setAge(38));
    Iterator<Person> people = datastore.find(Person.class).find();
    assertThat(people)
        .toIterable()
        .extracting(Person::getName, Person::getAge)
        .containsExactly(tuple("John Doe", 42), tuple("Jane Doe", 38));
  }

  private Datastore getDatastore() {
    return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntityScanPackage("java.lang")
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
