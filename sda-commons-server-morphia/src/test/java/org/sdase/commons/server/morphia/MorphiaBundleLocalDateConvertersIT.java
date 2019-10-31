package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
                      c ->
                          c.getMongo()
                              .setHosts(MONGODB.getHost())
                              .setDatabase(MONGODB.getDatabase()))
                  .build());

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

  @Before
  public void cleanCollection() {
    MONGODB.clearCollections();
  }

  @Test
  public void supportLocalDateAndLocalDateTime() {
    LocalDate birthday = LocalDate.of(1979, 2, 8);
    LocalDateTime lastLogin = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    getSdaDatastore()
        .save(new Person().setName("Peter Parker").setBirthday(birthday).setLastLogin(lastLogin));

    Person foundPerson =
        getSdaDatastore().createQuery(Person.class).field("name").equal("Peter Parker").first();

    assertThat(foundPerson).isNotNull();
    assertThat(foundPerson.getName()).isEqualTo("Peter Parker");
    assertThat(foundPerson.getBirthday()).isEqualTo(birthday);
    assertThat(foundPerson.getLastLogin()).isEqualTo(lastLogin);
  }

  @Test
  public void supportLocalDateRaw() {
    LocalDate birthday = LocalDate.of(1979, 2, 8);
    getSdaDatastore().save(new Person().setName("Peter Parker").setBirthday(birthday));

    Document foundPerson =
        MONGODB
            .createClient()
            .getDatabase(MONGODB.getDatabase())
            .getCollection("people")
            .find()
            .first();

    assertThat(foundPerson).isNotNull();
    assertThat(foundPerson.get("birthday")).isEqualTo("1979-02-08");
    assertThat(foundPerson.get("name")).isEqualTo("Peter Parker");
  }

  /**
   * This test demonstrates how the change of PR #185 breaks backward compatibility for {@link
   * LocalDate} fields used in entities saved with Morphia.
   */
  @Test
  public void demonstrateLocalDateBreakOfBackwardCompatibility() {

    Person given = new Person().setBirthday(LocalDate.of(1979, 2, 8)).setName("Peter Parker");
    getPlainMorphiaDatastore().save(given);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                getSdaDatastore()
                    .createQuery(Person.class)
                    .field("name")
                    .equal("Peter Parker")
                    .first())
        .withMessageContaining("java.util.Date");
  }

  private Datastore getSdaDatastore() {
    return ((MorphiaTestApp) DW.getRule().getApplication())
        .getMorphiaBundleWithSdaConverter()
        .datastore();
  }

  private Datastore getPlainMorphiaDatastore() {
    return ((MorphiaTestApp) DW.getRule().getApplication())
        .getMorphiaBundleWithPlainMorphia()
        .datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundleWithSdaConverter =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .build();

    private MorphiaBundle<Config> morphiaBundleWithPlainMorphia =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .disableDefaultTypeConverters()
            .build();

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
      // TODO: This is problematic. See {@link
      // MorphiaBundle#registerHealthCheck(HealthCheckRegistry, String)}.
      bootstrap.addBundle(morphiaBundleWithSdaConverter);
      bootstrap.addBundle(morphiaBundleWithPlainMorphia);
    }

    @Override
    public void run(Config configuration, Environment environment) {
      // nothing to run
    }

    MorphiaBundle<Config> getMorphiaBundleWithSdaConverter() {
      return morphiaBundleWithSdaConverter;
    }

    MorphiaBundle<Config> getMorphiaBundleWithPlainMorphia() {
      return morphiaBundleWithPlainMorphia;
    }
  }
}
