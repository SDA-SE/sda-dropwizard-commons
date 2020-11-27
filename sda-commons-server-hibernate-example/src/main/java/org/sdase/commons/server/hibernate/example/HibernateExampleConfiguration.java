package org.sdase.commons.server.hibernate.example;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class HibernateExampleConfiguration extends Configuration {

  /**
   * configuration required for hibernate database access. This section is mandatory within the
   * configuration if you use the hibernate bundle
   */
  private DataSourceFactory database = new DataSourceFactory();

  public DataSourceFactory getDatabase() {
    return database;
  }

  public void setDatabase(DataSourceFactory database) {
    this.database = database;
  }
}
