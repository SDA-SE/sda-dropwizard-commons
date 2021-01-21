package org.sdase.commons.server.morphia.example;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import dev.morphia.Datastore;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.morphia.example.mongo.CarManager;
import org.sdase.commons.server.morphia.example.mongo.model.Car;

/** Demonstration how to use the {@link MongoDbClassExtension}. */
class MorphiaApplicationJUnit5IT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension MONGODB =
      MongoDbClassExtension.builder()
          .withUsername("test-user")
          .withPassword("test-password")
          .withDatabase("test-database")
          .build(); // start a flapdoodle mongodb instance for this test. A random port is used.

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MorphiaApplicationConfiguration> APP_EXTENSION =
      new DropwizardAppExtension<>(
          MorphiaApplication.class,
          null,
          // start the application with random ports
          randomPorts(),
          // provide a lambda to only read the value after the mongodb connection parameters are
          // available
          config("mongo.hosts", MONGODB::getHosts),
          config("mongo.database", MONGODB::getDatabase),
          config("mongo.username", MONGODB::getUsername),
          config("mongo.password", MONGODB::getPassword),
          config("mongo.options", MONGODB::getOptions));

  private static final Car HH = new Car().setColor("green").setModel("BMW").setSign("HH-AA 123");
  private static final Car WL = new Car().setColor("purple").setModel("VW").setSign("WL-ZZ 9876");

  private CarManager carManager;
  private Datastore datastore;

  @BeforeEach
  public void before() {
    MorphiaApplication app = APP_EXTENSION.getApplication();
    carManager = app.carManager();
    datastore = app.morphiaDatastore();
    datastore.delete(datastore.createQuery(Car.class));
  }

  @Test
  void shouldStoreCarEntity() {
    addData();
    assertThat(datastore.createQuery(Car.class).count()).isEqualTo(2);
    assertThat(datastore.createQuery(Car.class).find().toList())
        .usingFieldByFieldElementComparator()
        .contains(WL, HH);
  }

  @Test
  void shouldReadHHEntitiesOnly() {
    addData();
    assertThat(carManager.hamburgCars()).usingFieldByFieldElementComparator().containsExactly(HH);
  }

  @Test
  void shouldHaveIndexOnSign() {
    Iterable<Document> indexInfo = datastore.getDatabase().getCollection("cars").listIndexes();
    assertThat(indexInfo)
        .extracting(dbo -> dbo.get("name"))
        .containsExactlyInAnyOrder("_id_", "sign_1");
  }

  private void addData() {
    carManager.store(HH);
    carManager.store(WL);
  }
}
