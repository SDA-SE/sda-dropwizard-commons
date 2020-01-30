package org.sdase.commons.server.morphia.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.morphia.internal.ConnectionStringUtil.createConnectionString;

import org.junit.Test;
import org.sdase.commons.server.morphia.MongoConfiguration;

public class ConnectionStringUtilTest {

  @Test
  public void buildConnectionStringWithOneHost() {
    MongoConfiguration mongoConfiguration = createValidConfiguration().setHosts("only.example.com");

    String connectionString = createConnectionString(mongoConfiguration);

    assertThat(connectionString).isNotNull().contains("@only.example.com/");
  }

  @Test
  public void testUriBuilderUserPassword() {
    MongoConfiguration mongoConfiguration = createValidConfiguration();

    String connectionString = createConnectionString(mongoConfiguration);

    assertThat(connectionString)
        .isNotNull()
        .isEqualTo(
            "mongodb://dbuser:sda123@db1.example.net:27017,db2.example.net:2500/default_db?replicaSet=test");
  }

  @Test
  public void testUriBuilderWithoutUserPassword() {
    MongoConfiguration mongoConfiguration = createValidConfiguration();
    mongoConfiguration.setUsername(null);
    mongoConfiguration.setPassword(null);

    String connectionString = createConnectionString(mongoConfiguration);

    assertThat(connectionString)
        .isNotNull()
        .isEqualTo(
            "mongodb://db1.example.net:27017,db2.example.net:2500/default_db?replicaSet=test");
  }

  @Test
  public void testUriBuilderWithoutOptions() {
    MongoConfiguration mongoConfiguration = createValidConfiguration();
    mongoConfiguration.setOptions(null);

    String connectionString = createConnectionString(mongoConfiguration);

    assertThat(connectionString)
        .isNotNull()
        .isEqualTo("mongodb://dbuser:sda123@db1.example.net:27017,db2.example.net:2500/default_db");
  }

  private static MongoConfiguration createValidConfiguration() {

    MongoConfiguration mongoConfiguration = new MongoConfiguration();

    mongoConfiguration.setDatabase("default_db");
    mongoConfiguration.setHosts("db1.example.net:27017,db2.example.net:2500");
    mongoConfiguration.setOptions("replicaSet=test");
    mongoConfiguration.setUsername("dbuser");
    mongoConfiguration.setPassword("sda123");

    return mongoConfiguration;
  }
}
