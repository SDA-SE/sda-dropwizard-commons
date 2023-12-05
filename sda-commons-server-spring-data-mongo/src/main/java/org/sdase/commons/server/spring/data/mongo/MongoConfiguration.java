package org.sdase.commons.server.spring.data.mongo;

import jakarta.validation.constraints.NotBlank;

public class MongoConfiguration {

  /**
   * Plain MongoDB connection string as described in the <a
   * href="https://www.mongodb.com/docs/manual/reference/connection-string/">official
   * documentation</a>.
   */
  @NotBlank private String connectionString;

  public String getConnectionString() {
    return connectionString;
  }

  public MongoConfiguration setConnectionString(String connectionString) {
    this.connectionString = connectionString;
    return this;
  }
}
