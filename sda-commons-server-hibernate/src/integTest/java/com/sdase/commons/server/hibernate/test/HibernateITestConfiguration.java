package com.sdase.commons.server.hibernate.test;

import com.sdase.commons.server.hibernate.DatabaseConfigurable;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class HibernateITestConfiguration extends Configuration implements DatabaseConfigurable {

   private DataSourceFactory database;

   public void setDatabase(DataSourceFactory database) {
      this.database = database;
   }

   @Override
   public DataSourceFactory getDatabase() {
      return database;
   }
}
