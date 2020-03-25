package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThatCode;

import dev.morphia.Datastore;
import dev.morphia.VerboseJSR303ConstraintViolationException;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

/** Tests if entities are validated. */
public class MorphiaBundleValidationIT {

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

  @Test(expected = VerboseJSR303ConstraintViolationException.class)
  public void shouldNotStoreInvalidPerson() {
    Datastore datastore = getDatastore();
    datastore.save(new Person());
  }

  @Test
  public void shouldStoreValidPerson() {
    assertThatCode(
            () -> {
              Datastore datastore = getDatastore();
              datastore.save(new Person().setName("Name"));
            })
        .doesNotThrowAnyException();
  }

  private Datastore getDatastore() {
    return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle().datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .withValidation()
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
