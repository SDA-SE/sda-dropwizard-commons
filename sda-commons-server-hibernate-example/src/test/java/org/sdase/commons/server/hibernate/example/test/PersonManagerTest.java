package org.sdase.commons.server.hibernate.example.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.hibernate.example.test.PersonManagerTest.PWD;
import static org.sdase.commons.server.hibernate.example.test.PersonManagerTest.URL;
import static org.sdase.commons.server.hibernate.example.test.PersonManagerTest.USER;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import io.dropwizard.testing.junit5.DAOTestExtension;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.hibernate.example.db.manager.PersonManager;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;

// add connection information to ride db test util
@DBUnit(url = URL, driver = "org.h2.Driver", user = USER, password = PWD)
@ExtendWith({DBUnitExtension.class})
class PersonManagerTest {

  // define connect information
  static final String URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
  static final String USER = "sa";
  static final String PWD = "sa";

  /** DAOTest rule creates the database and provide means to access it */
  @RegisterExtension
  private final DAOTestExtension daoTestExtension =
      DAOTestExtension.newBuilder()
          .setProperty(AvailableSettings.URL, URL)
          .setProperty(AvailableSettings.USER, USER)
          .setProperty(AvailableSettings.PASS, PWD)
          .setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop")
          .setProperty(AvailableSettings.AUTO_CLOSE_SESSION, "false")
          .setProperty(AvailableSettings.AUTOCOMMIT, "true")
          .addEntityClass(PersonEntity.class)
          .build();

  private PersonManager personManager;

  @BeforeAll
  public static void initDb() {
    // init database with schema
    Flyway flyway = new Flyway(new FluentConfiguration().dataSource(URL, USER, PWD));
    flyway.migrate();
  }

  @BeforeEach
  void before() throws Throwable {
    daoTestExtension.before();
    personManager = new PersonManager(daoTestExtension.getSessionFactory());
  }

  @Test
  @DataSet("datasets/base.yml") // initialize database with data from yml file
  void testGetById() {
    PersonEntity byId = personManager.getById(1);
    assertThat(byId.getName()).isEqualTo("John Doe");
  }

  @Test
  @DataSet("datasets/base.yml") // initialize database with data from yml file
  @ExpectedDataSet({
    "datasets/base.yml",
    "datasets/added.yml"
  }) // assert that database reflects the given data
  void testCreation() { // NOSONAR the assertion is fulfilled by DBUnit with @ExpectedDataSet
    PersonEntity person = new PersonEntity();
    person.setName("Jasmin Doe");
    daoTestExtension.inTransaction(() -> personManager.persist(person));
  }
}
