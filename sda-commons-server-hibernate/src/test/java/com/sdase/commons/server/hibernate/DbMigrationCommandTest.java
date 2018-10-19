package com.sdase.commons.server.hibernate;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.junit.DAOTestRule;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.SessionImpl;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.Assert.assertTrue;

public class DbMigrationCommandTest {

   private static final String DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

   @Rule
   public final DAOTestRule daoTestRule = DAOTestRule
         .newBuilder()
         .setProperty(AvailableSettings.URL, DB_URL)
         .setProperty(AvailableSettings.USER, "sa")
         .setProperty(AvailableSettings.PASS, "sa")
         .setProperty(AvailableSettings.DEFAULT_SCHEMA, "public")
         .build();

   @Test
   public void testDBMigration() throws Exception {
      // given
      DbMigrationCommand migrationCommand = new DbMigrationCommand();
      DataSourceFactory dataSourceFactory = new DataSourceFactory();
      dataSourceFactory.setUrl(DB_URL);
      dataSourceFactory.setUser("sa");
      dataSourceFactory.setPassword("sa");
      DbMigrationConfiguration config = new DbMigrationConfiguration();
      config.setDatabase(dataSourceFactory);

      // when
      migrationCommand.run(null, null, config);

      // then see annotation
      Connection connection = ((SessionImpl) daoTestRule.getSessionFactory().getCurrentSession()).connection();
      ResultSet tables = connection.getMetaData().getTables("", "public", "schema_version", null);
      assertTrue("Expect a result for schema_version", tables.first());
   }

}
