package com.sdase.commons.server.hibernate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

/**
 * Database configuration class with explicit type used for the {@link DbMigrationCommand}. This configuration takes the
 * necessary properties for DB migration and ignores other properties that might be used in a specific application.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DbMigrationConfiguration extends Configuration implements DatabaseConfigurable {

   private DataSourceFactory database;

   @Override
   public DataSourceFactory getDatabase() {
      return database;
   }

   public DbMigrationConfiguration setDatabase(DataSourceFactory database) {
      this.database = database;
      return this;
   }
}
