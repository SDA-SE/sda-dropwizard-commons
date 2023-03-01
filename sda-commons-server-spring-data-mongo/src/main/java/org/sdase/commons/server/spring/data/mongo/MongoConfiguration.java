package org.sdase.commons.server.spring.data.mongo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.ConnectionString;
import javax.validation.constraints.AssertTrue;
import org.apache.commons.lang3.StringUtils;

public class MongoConfiguration {

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
   *
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  private String hosts;

  /**
   * The name of the mongo database to access.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for database.
   *
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  private String database;

  /**
   * Additional options for the connection.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for options.
   *
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  private String options = "";

  /**
   * The username used for login at the MongoDB.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for username:password.
   *
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  private String username;

  /**
   * The password used for login at the MongoDB.
   *
   * <p>Details in the <a
   * href="https://docs.mongodb.com/manual/reference/connection-string/">connection string
   * documentation</a> for username:password.
   *
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  private String password;

  /**
   * If SSL should be used for the database connection.
   *
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  private boolean useSsl;

  /**
   * Plain MongoDB connection string as described in the <a
   * href="https://www.mongodb.com/docs/manual/reference/connection-string/">official
   * documentation</a>.
   */
  private String connectionString;

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public String getHosts() {
    if (StringUtils.isBlank(hosts) && StringUtils.isNotBlank(connectionString)) {
      return String.join(",", new ConnectionString(connectionString).getHosts());
    }
    return hosts;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public MongoConfiguration setHosts(String hosts) {
    this.hosts = hosts;
    return this;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public String getDatabase() {
    if (StringUtils.isBlank(database) && StringUtils.isNotBlank(connectionString)) {
      return String.join(",", new ConnectionString(connectionString).getDatabase());
    }
    return database;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public MongoConfiguration setDatabase(String database) {
    this.database = database;
    return this;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public String getOptions() {
    return options;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public MongoConfiguration setOptions(String options) {
    this.options = options;
    return this;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public String getUsername() {
    if (StringUtils.isBlank(username) && StringUtils.isNotBlank(connectionString)) {
      return String.join(",", new ConnectionString(connectionString).getUsername());
    }
    return username;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public MongoConfiguration setUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public String getPassword() {
    if (StringUtils.isBlank(password) && StringUtils.isNotBlank(connectionString)) {
      char[] passwordChars = new ConnectionString(connectionString).getPassword();
      if (passwordChars != null) {
        return new String(passwordChars);
      }
    }
    return password;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public MongoConfiguration setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public boolean isUseSsl() {
    return useSsl;
  }

  /**
   * @deprecated All the Mongo config options will be removed in favor of 'connectionString'.
   */
  @Deprecated(forRemoval = true)
  public MongoConfiguration setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
    return this;
  }

  public String getConnectionString() {
    return connectionString;
  }

  public MongoConfiguration setConnectionString(String connectionString) {
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
