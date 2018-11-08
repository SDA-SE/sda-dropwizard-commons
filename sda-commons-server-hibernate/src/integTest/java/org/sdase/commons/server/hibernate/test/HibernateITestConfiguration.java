package org.sdase.commons.server.hibernate.test;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class HibernateITestConfiguration extends Configuration {

   private DataSourceFactory database;

   private String propertyForOtherBundle;

   public void setDatabase(DataSourceFactory database) {
      this.database = database;
   }

   public DataSourceFactory getDatabase() {
      return database;
   }

   public String getPropertyForOtherBundle() {
      return propertyForOtherBundle;
   }

   public HibernateITestConfiguration setPropertyForOtherBundle(String propertyForOtherBundle) {
      this.propertyForOtherBundle = propertyForOtherBundle;
      return this;
   }
}
