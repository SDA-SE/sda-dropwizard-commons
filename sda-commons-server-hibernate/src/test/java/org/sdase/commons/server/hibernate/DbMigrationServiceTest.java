package org.sdase.commons.server.hibernate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.junit5.DBUnitExtension;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.junit5.DAOTestExtension;
import java.sql.Connection;
import java.sql.ResultSet;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.SessionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@DBUnit(url = DbMigrationServiceTest.DB_URL, driver = "org.h2.Driver", user = "sa", password = "sa")
@ExtendWith(DBUnitExtension.class)
class DbMigrationServiceTest {

  public static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

  @RegisterExtension
  DAOTestExtension daoTestExtension =
      DAOTestExtension.newBuilder()
          .setProperty(AvailableSettings.URL, DB_URL)
          .setProperty(AvailableSettings.USER, "sa")
          .setProperty(AvailableSettings.PASS, "sa")
          .setProperty(AvailableSettings.DEFAULT_SCHEMA, "public")
          .build();

  @Test
  void testDBMigration() throws Exception {
    // given
    DataSourceFactory dataSourceFactory = new DataSourceFactory();
    dataSourceFactory.setUrl(DB_URL);
    dataSourceFactory.setUser("sa");
    dataSourceFactory.setPassword("sa");

    // when
    new DbMigrationService(dataSourceFactory).migrateDatabase();
    // then see annotation
    Connection connection =
        ((SessionImpl) daoTestExtension.getSessionFactory().openSession()).connection();
    ResultSet tables =
        connection.getMetaData().getTables("", "public", "flyway_schema_history", null);
    assertTrue(tables.first(), "Expect a result for schema_version");
  }
}
