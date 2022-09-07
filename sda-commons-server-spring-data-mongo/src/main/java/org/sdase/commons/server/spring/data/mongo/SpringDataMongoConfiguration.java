package org.sdase.commons.server.spring.data.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.ConnectionString;
import javax.validation.constraints.AssertTrue;
import org.apache.commons.lang3.StringUtils;

public class SpringDataMongoConfiguration {

  /**
   * Comma separated list of hosts with their port that build the MongoDB cluster:
   *
   * <pre>{@code mongo-db-a:27018,mongo-db-b:27018,mongo-db-c:27018}</pre>
   *
   * <p>The default port if no port is specified is {@code :27017}:
   *
   * <p>{@code mongo-db-a,mongo-db-b,mongo-db-c} is equal to {@code
   * mongo-db-a:27017,mongo-db-b:27017,mongo-db-c:27017}
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for host1 to hostN.
   */
  private String hosts;

  /**
   * The name of the mongo database to access.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for database.
   */
  private String database;

  /**
   * Additional options for the connection.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for options.
   */
  private String options = "";

  /**
   * The username used for login at the MongoDB.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for username:password.
   */
  private String username;

  /**
   * The password used for login at the MongoDB.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for username:password.
   */
  private String password;

  /** If SSL should be used for the database connection. */
  private boolean useSsl;

  private String connectionString;

  public String getHosts() {
    if (StringUtils.isBlank(hosts) && StringUtils.isNotBlank(connectionString)) {
      return String.join(",", new ConnectionString(connectionString).getHosts());
    }
    return hosts;
  }

  public SpringDataMongoConfiguration setHosts(String hosts) {
    this.hosts = hosts;
    return this;
  }

  public String getDatabase() {
    if (StringUtils.isBlank(database) && StringUtils.isNotBlank(connectionString)) {
      return String.join(",", new ConnectionString(connectionString).getDatabase());
    }
    return database;
  }

  public SpringDataMongoConfiguration setDatabase(String database) {
    this.database = database;
    return this;
  }

  public String getOptions() {
    if (StringUtils.isBlank(options) && StringUtils.isNotBlank(connectionString)) {
      // TODO what should be returned?
    }
    return options;
  }

  public SpringDataMongoConfiguration setOptions(String options) {
    this.options = options;
    return this;
  }

  public String getUsername() {
    if (StringUtils.isBlank(username) && StringUtils.isNotBlank(connectionString)) {
      return String.join(",", new ConnectionString(connectionString).getUsername());
    }
    return username;
  }

  public SpringDataMongoConfiguration setUsername(String username) {
    this.username = username;
    return this;
  }

  public String getPassword() {
    if (StringUtils.isBlank(password) && StringUtils.isNotBlank(connectionString)) {
      char[] passwordChars = new ConnectionString(connectionString).getPassword();
      if (passwordChars != null) {
        return new String(passwordChars);
      }
    }
    return password;
  }

  public SpringDataMongoConfiguration setPassword(String password) {
    this.password = password;
    return this;
  }

  public boolean isUseSsl() {
    return useSsl;
  }

  public SpringDataMongoConfiguration setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
    return this;
  }

  public String getConnectionString() {
    return connectionString;
  }

  public SpringDataMongoConfiguration setConnectionString(String connectionString) {
    this.connectionString = connectionString;
    return this;
  }

  /**
   * We either need the
   *
   * <ul>
   *   <li>'connectionString'
   *   <li>'hosts' and 'database'
   * </ul>
   *
   * @return true if the configuration is valid
   */
  @AssertTrue
  @JsonIgnore
  public boolean isValid() {
    if (StringUtils.isNotBlank(connectionString)) {
      return true;
    }

    return StringUtils.isNotBlank(hosts) && StringUtils.isNotBlank(database);
  }
}
