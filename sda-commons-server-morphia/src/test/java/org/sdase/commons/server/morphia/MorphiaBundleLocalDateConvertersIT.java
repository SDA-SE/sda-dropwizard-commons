package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThat;

import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

/** Tests if entities can be added by exact definition. */
public class MorphiaBundleLocalDateConvertersIT {

  private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

  private static final LazyRule<DropwizardAppRule<Config>> DW =
      new LazyRule<>(
          () ->
              DropwizardRuleHelper.dropwizardTestAppFrom(MorphiaTestApp.class)
                  .withConfigFrom(Config::new)
                  .withRandomPorts()
                  .withConfigurationModifier(
                      c -> c.getMongo().setHosts(MONGODB.getHost()).setDatabase("testPeople"))
                  .build());

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

  @Test
  public void supportLocalDateAndLocalDateTime() {
    Datastore datastore = getDatastore();
    LocalDate birthday = LocalDate.of(1979, 2, 8);
    LocalDateTime lastLogin = LocalDateTime.now().withNano(0); // Mongo precision
    datastore.save(
        new Person().setName("Peter Parker").setBirthday(birthday).setLastLogin(lastLogin));

    Person foundPerson =
        datastore.createQuery(Person.class).field("name").equal("Peter Parker").first();

    assertThat(foundPerson).isNotNull();
    assertThat(foundPerson.getName()).isEqualTo("Peter Parker");
    assertThat(foundPerson.getBirthday()).isEqualTo(birthday);
    assertThat(foundPerson.getLastLogin()).isEqualTo(lastLogin);
  }

  private Datastore getDatastore() {
    return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().datastore();
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
