package org.sdase.commons.server.mongo.testing;

import com.mongodb.ConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseExistingMongoDb implements MongoDb {

  private static final Logger LOG = LoggerFactory.getLogger(UseExistingMongoDb.class);

  private final ConnectionString mongoConnectionString;

  public UseExistingMongoDb(String mongoDbConnectionString) {
    this.mongoConnectionString = new ConnectionString(mongoDbConnectionString);
  }

  public String getConnectionString() {
    return mongoConnectionString.getConnectionString();
  }

  public ConnectionString getMongoConnectionString() {
    return mongoConnectionString;
  }

  protected void logConnection() {
    if (LOG.isInfoEnabled()) {
      String withPasswordText =
          mongoConnectionString.getPassword() != null ? "using a password" : "without password";
      LOG.info(
          "Testing as '{}' {} in MongoDB '{}' at '{}' with options '{}'",
          mongoConnectionString.getUsername(),
          withPasswordText,
          mongoConnectionString.getDatabase(),
          mongoConnectionString.getHosts(),
          optionsFromMongoUrl(getConnectionString()));
      // secondary log, getServerVersion() already requires a connection
      LOG.info("MongoDB server used for test has version {}", getServerVersion());
    }
  }

  private String optionsFromMongoUrl(String mongoDbUrlOverride) {
    String[] split = mongoDbUrlOverride.split("\\?");
    if (split.length > 1) {
      return split[split.length - 1];
    } else {
      return "";
    }
  }
}
