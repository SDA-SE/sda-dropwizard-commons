package org.sdase.commons.server.morphia.example;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.example.mongo.CarManager;
import org.sdase.commons.server.morphia.example.mongo.model.Car;
import org.sdase.commons.server.testing.DropwizardConfigurationHelper;
import org.sdase.commons.server.testing.LazyRule;
import org.sdase.commons.server.weld.testing.WeldAppRule;
import xyz.morphia.Datastore;

import static org.assertj.core.api.Assertions.assertThat;

public class MorphiaApplicationIT {

   private static final MongoDbRule MONGODB = MongoDbRule.builder().build(); // start a flapdoodle mongodb instance for this test. A random port is used.

   private static final LazyRule<WeldAppRule> LAZY_RULE = new LazyRule<>(() -> // use lazy rule to initialize application so that mongo connection parameters are available
         new WeldAppRule<>(MorphiaApplication.class, //normal WELD rule initialization
               DropwizardConfigurationHelper.configFrom(MorphiaApplicationConfiguration::new) // set mongo parameters dynamically
                     .withRandomPorts()
                     .withConfigurationModifier(c -> c.getMongo()
                           .setHosts(MONGODB.getHost())
                           .setDatabase(MongoDbRule.Builder.DEFAULT_DATABASE)
                     ).build()
         ));

   @ClassRule
   public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(LAZY_RULE); // initialize the test environment

   private static final Car HH = new Car().setColor("green").setModel("BMW").setSign("HH-AA 123");
   private static final Car WL = new Car().setColor("purple").setModel("VW").setSign("WL-ZZ 9876");

   private CarManager carManager;
   private Datastore datastore;

   @Before
   public void before() {
      MorphiaApplication app = (MorphiaApplication) LAZY_RULE.getRule().getApplication();
      carManager = app.carManager();
      datastore = app.morphiaDatastore();
      datastore.getCollection(Car.class).drop();
   }

   @Test
   public void shouldStoreCarEntity() {
      addData();
      assertThat(datastore.getCollection(Car.class).count()).isEqualTo(2);
      assertThat(datastore.createQuery(Car.class).asList()).usingFieldByFieldElementComparator().contains(WL, HH);
   }

   @Test
   public void shouldReadHHEntitiesOnly() {
      addData();
      assertThat(carManager.hamburgCars()).usingFieldByFieldElementComparator().containsExactly(HH);
   }

   private void addData() {
      carManager.store(HH);
      carManager.store(WL);
   }



}
