package org.sdase.commons.server.hibernate.testing;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.hibernate.testing.db.manager.PersonManager;
import org.sdase.commons.server.hibernate.testing.db.model.PersonEntity;

// add connection information to ride db test util
@DBUnit(
    url = PersonManagerJUnit5IT.URL,
    driver = "org.h2.Driver",
    user = PersonManagerJUnit5IT.USER,
    password = PersonManagerJUnit5IT.PWD)
public class PersonManagerJUnit5IT {

  // define connect information
  static final String URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
  static final String USER = "sa";
  static final String PWD = "sa";

  @Order(0)
  @RegisterExtension
  static final DBUnitExtension dbUnitExtension = new DBUnitExtension();

  /** DAOClassExtension creates the database and provide means to access it */
  @Order(1)
  @RegisterExtension
  static final DAOClassExtension daoClassExtension =
      DAOClassExtension.newBuilder()
          .setProperty(AvailableSettings.URL, URL)
          .setProperty(AvailableSettings.USER, USER)
          .setProperty(AvailableSettings.PASS, PWD)
          .setProperty(AvailableSettings.HBM2DDL_AUTO, "create-drop")
          .addEntityClass(PersonEntity.class)
          .build();

  private PersonManager personManager;

  @BeforeAll
  public static void initDb() {
    // init database with schema
    Flyway flyway =
        new Flyway(new FluentConfiguration().baselineOnMigrate(true).dataSource(URL, USER, PWD));
    flyway.migrate();
  }

  @BeforeEach
  public void before() {
    personManager = new PersonManager(daoClassExtension.getSessionFactory());
  }

  @Test
  @DataSet("datasets/base.yml") // initialize database with data from yml file
  void testGetById() {
    PersonEntity byId = personManager.getById(1);
    Assertions.assertThat(byId.getName()).isEqualTo("John Doe");
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
    daoClassExtension.inTransaction(() -> personManager.persist(person));
  }
}
