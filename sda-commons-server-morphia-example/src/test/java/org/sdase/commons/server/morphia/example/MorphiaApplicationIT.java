package org.sdase.commons.server.morphia.example;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import dev.morphia.Datastore;
import org.bson.Document;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.example.mongo.CarManager;
import org.sdase.commons.server.morphia.example.mongo.model.Car;
import org.sdase.commons.server.weld.testing.WeldAppRule;

public class MorphiaApplicationIT {

  private static final MongoDbRule MONGODB =
      MongoDbRule.builder()
          .build(); // start a flapdoodle mongodb instance for this test. A random port is used.

  private static final WeldAppRule<MorphiaApplicationConfiguration> APP_RULE =
      new WeldAppRule<>(
          MorphiaApplication.class, // normal WELD rule initialization
          null,
          // start the application with random ports
          randomPorts(),
          // provide a lambda to only read the value after the mongodb connection parameters are
          // available
          config("mongo.hosts", MONGODB::getHost),
          config("mongo.database", MONGODB::getDatabase),
          config("mongo.username", MONGODB::getUsername),
          config("mongo.password", MONGODB::getPassword),
          config("mongo.options", MONGODB::getOptions));

  @ClassRule
  public static final RuleChain CHAIN =
      RuleChain.outerRule(MONGODB).around(APP_RULE); // initialize the test environment

  private static final Car HH = new Car().setColor("green").setModel("BMW").setSign("HH-AA 123");
  private static final Car WL = new Car().setColor("purple").setModel("VW").setSign("WL-ZZ 9876");

  private CarManager carManager;
  private Datastore datastore;

  @Before
  public void before() {
    MorphiaApplication app = APP_RULE.<MorphiaApplication>getApplication();
    carManager = app.carManager();
    datastore = app.morphiaDatastore();
    datastore.delete(datastore.createQuery(Car.class));
  }

  @Test
  public void shouldStoreCarEntity() {
    addData();
    assertThat(datastore.createQuery(Car.class).count()).isEqualTo(2);
    assertThat(datastore.createQuery(Car.class).find().toList())
        .usingFieldByFieldElementComparator()
        .contains(WL, HH);
  }

  @Test
  public void shouldReadHHEntitiesOnly() {
    addData();
    assertThat(carManager.hamburgCars()).usingFieldByFieldElementComparator().containsExactly(HH);
  }

  @Test
  public void shouldHaveIndexOnSign() {
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
