package org.sdase.commons.server.spring.data.mongo.inheritance;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.SpringDataMongoBundle;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.inheritance.model.AbstractPet.Cat;
import org.sdase.commons.server.spring.data.mongo.inheritance.model.AbstractPet.Dog;
import org.sdase.commons.server.spring.data.mongo.inheritance.model.PrivateZoo;
import org.springframework.data.mongodb.core.MongoOperations;

class InheritanceMappingITest {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension MONGO = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyConfiguration> DW =
      new DropwizardAppExtension<>(
          ZooTestApp.class,
          null,
          config("springDataMongo.connectionString", MONGO::getConnectionString));

  MongoOperations mongoOperations;

  @BeforeEach
  void setUp() {
    MONGO.clearCollections();
    ZooTestApp app = DW.getApplication();
    mongoOperations = app.getMongoOperations();
  }

  @Test
  void shouldStorePrivateZoo() {
    var given =
        new PrivateZoo().setPets(List.of(new Dog().setName("Wuff"), new Cat().setName("Miez")));

    mongoOperations.insert(given);

    try (var mongoClient = MONGO.createClient()) {
      Document privateZoo =
          mongoClient.getDatabase(MONGO.getDatabase()).getCollection("zoo").find().first();

      assertThat(privateZoo)
          .isNotNull()
          .extracting("pets")
          .asList()
          .extracting("name", "_class")
          .containsExactly(tuple("Wuff", "dog"), tuple("Miez", "cat"));
    }
  }

  @Test
  void shouldReadWhatItStored() {
    var given =
        new PrivateZoo().setPets(List.of(new Dog().setName("Wuff"), new Cat().setName("Miez")));

    mongoOperations.insert(given);

    var actual = mongoOperations.findAll(PrivateZoo.class);
    assertThat(actual).hasSize(1);

    var privateZoo = actual.get(0);

    SoftAssertions.assertSoftly(
        softly -> {
          softly
              .assertThat(privateZoo)
              .isNotNull()
              .extracting(PrivateZoo::getPets)
              .asList()
              .extracting("name")
              .containsExactly("Wuff", "Miez");
          softly.assertThat(privateZoo.getPets().get(0)).isExactlyInstanceOf(Dog.class);
          softly.assertThat(privateZoo.getPets().get(1)).isExactlyInstanceOf(Cat.class);
        });
  }

  @Test
  void shouldReadWithWrongClassProperty() {
    var given =
        new Document(
            Map.of(
                "_id",
                "12345abcde",
                "_class",
                "org.sdase.commons.refactored.model.Zoo",
                "pets",
                List.of(
                    Map.of("_class", "dog", "name", "Wuff"),
                    Map.of("_class", "cat", "name", "Miez"))));

    try (var mongoClient = MONGO.createClient()) {
      mongoClient.getDatabase(MONGO.getDatabase()).getCollection("zoo").insertOne(given);

      List<PrivateZoo> all = mongoOperations.findAll(PrivateZoo.class);
      assertThat(all).hasSize(1);
      assertThat(all.get(0))
          .isExactlyInstanceOf(PrivateZoo.class)
          .extracting(PrivateZoo::getId)
          .isEqualTo("12345abcde");
    }
  }

  public static class ZooTestApp extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(PrivateZoo.class, Dog.class, Cat.class)
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
}
