package org.sdase.commons.server.mongo.testing;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseExistingMongoDbRule extends ExternalResource implements MongoDbRule {

  private static final Logger LOG = LoggerFactory.getLogger(UseExistingMongoDbRule.class);

  private final String host;
  private final String database;
  private final String username;
  private final String password;
  private final String options;
  private final MongoClientURI mongoClientUri;

  public UseExistingMongoDbRule(String mongoDbConnectionString) {
    mongoClientUri = new MongoClientURI(mongoDbConnectionString);
    host = String.join(",", mongoClientUri.getHosts());
    database = mongoClientUri.getDatabase();
    options = optionsFromMongoUrl(mongoDbConnectionString);
    username = mongoClientUri.getUsername();
    password = asStringOrNull(mongoClientUri.getPassword());
  }

  @Override
  public String getHosts() {
    return host;
  }

  @Override
  public String getDatabase() {
    return database;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getOptions() {
    return options;
  }

  @Override
  public MongoClient createClient() {
    return new MongoClient(mongoClientUri);
  }

  @Override
  protected void before() {
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
