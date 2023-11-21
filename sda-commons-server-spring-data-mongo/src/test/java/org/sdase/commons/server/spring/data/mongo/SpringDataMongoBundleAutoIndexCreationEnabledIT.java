package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.mongo.distribution.Version;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.springframework.data.mongodb.core.MongoOperations;

abstract class SpringDataMongoBundleAutoIndexCreationEnabledIT {

  static class MongoDb44Test extends SpringDataMongoBundleAutoIndexCreationEnabledIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            AutoIndexEnabledApp.class,
            null,
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  static class MongoDb50Test extends SpringDataMongoBundleAutoIndexCreationEnabledIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            AutoIndexEnabledApp.class,
            null,
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  @Test
  void shouldHaveCreatedIndexed() {
    MongoOperations mongoOperations = getMongoOperations();
    var indexesByMongoClient = mongoOperations.getCollection("people").listIndexes();
    assertThat(indexesByMongoClient).hasSize(3);
  }

  private MongoOperations getMongoOperations() {
    return getDW().<AutoIndexEnabledApp>getApplication().getMongoOperations();
  }

  public static class AutoIndexEnabledApp extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(Person.class)
            .build();

    @Override
    public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      bootstrap.addBundle(springDataMongoBundle);
    }

    @Override
    public void run(MyConfiguration configuration, Environment environment) {
      // nothing to run
    }

    public MongoOperations getMongoOperations() {
      return springDataMongoBundle.getMongoOperations();
    }
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();
}
