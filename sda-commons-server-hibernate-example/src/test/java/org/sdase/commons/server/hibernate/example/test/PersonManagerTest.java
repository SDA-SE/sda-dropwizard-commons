package org.sdase.commons.server.hibernate.example.test;

import com.github.database.rider.core.DBUnitRule;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import io.dropwizard.testing.junit.DAOTestRule;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.server.hibernate.example.db.manager.PersonManager;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.hibernate.example.test.PersonManagerTest.*;

// add connection information to ride db test util
@DBUnit(url = URL, driver = "org.h2.Driver", user = USER, password = PWD)
public class PersonManagerTest {

   // define connect information
   static final String URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
   static final String USER = "sa";
   static final String PWD = "sa";


   /**
    * DAOTest rule creates the database and provide means to access it
    */
   private final DAOTestRule daoTestRule = DAOTestRule
         .newBuilder()
         .setProperty(AvailableSettings.URL, URL)
         .setProperty(AvailableSettings.USER, USER)
         .setProperty(AvailableSettings.PASS, PWD)
         .setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop")
         .addEntityClass(PersonEntity.class)
         .build();


   // Be sure to add the @DBUnitRule to your rule chain. Otherwise rider will not initialize your database
   @Rule
   public final TestRule chain = RuleChain.outerRule(daoTestRule).around(DBUnitRule.instance());

   private PersonManager personManager;

   @BeforeClass
   public static void initDb() {
      // init database with schema
      Flyway flyway = new Flyway();
      flyway.setDataSource(URL, USER, PWD);
      flyway.migrate();
   }

   @Before
   public void before() {
      personManager = new PersonManager(daoTestRule.getSessionFactory());
   }

   @Test
   @DataSet("datasets/base.yml") // initialize database with data from yml file
   public void testGetById() {
      PersonEntity byId = personManager.getById(1);
      assertThat(byId.getName()).isEqualTo("John Doe");
   }

   @Test
   @DataSet("datasets/base.yml")  // initialize database with data from yml file
   @ExpectedDataSet({"datasets/base.yml", "datasets/added.yml"}) // assert that database reflects the given data
   public void testCreation() {
      PersonEntity person = new PersonEntity();
      person.setName("Jasmin Doe");
      daoTestRule.inTransaction(() -> personManager.persist(person));

   }

}
