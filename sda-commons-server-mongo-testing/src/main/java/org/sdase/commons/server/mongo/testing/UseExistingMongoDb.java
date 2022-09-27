package org.sdase.commons.server.mongo.testing;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseExistingMongoDb {

  private static final Logger LOG = LoggerFactory.getLogger(UseExistingMongoDb.class);

  private final String hosts;
  private final String database;
  private final String username;
  private final String password;
  private final String options;
  private final MongoClientURI mongoClientUri;

  private String connectionString;

  public UseExistingMongoDb(String mongoDbConnectionString) {
    this.connectionString = mongoDbConnectionString;
    mongoClientUri = new MongoClientURI(mongoDbConnectionString);
    hosts = String.join(",", mongoClientUri.getHosts());
    database = mongoClientUri.getDatabase();
    options = optionsFromMongoUrl(mongoDbConnectionString);
    username = mongoClientUri.getUsername();
    password = asStringOrNull(mongoClientUri.getPassword());
  }

  public String getHosts() {
    return hosts;
  }

  public String getDatabase() {
    return database;
  }

  public String getOptions() {
    return options;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getConnectionString() {
    return connectionString;
  }

  public MongoClient createClient() {
    return new MongoClient(mongoClientUri);
  }

  /**
   * @return the version of the MongoDB instance which is associated with this MongoDbClassExtension
   */
  public String getServerVersion() {
    try (MongoClient client = createClient()) {
      return client
          .getDatabase(getDatabase())
          .runCommand(new BsonDocument("buildinfo", new BsonString("")))
          .get("version")
          .toString();
    }
  }

  protected void logConnection() {
    if (LOG.isInfoEnabled()) {
      String withPasswordText = isNotBlank(password) ? "using a password" : "without password";
      LOG.info(
          "Testing as '{}' {} in MongoDB '{}' at '{}' with options '{}'",
          username,
          withPasswordText,
          getDatabase(),
          getHosts(),
          getOptions());
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

  private String asStringOrNull(char[] chars) {
    return chars == null ? null : new String(chars);
  }
}
