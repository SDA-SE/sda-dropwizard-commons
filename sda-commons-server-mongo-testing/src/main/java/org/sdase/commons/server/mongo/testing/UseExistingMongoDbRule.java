package org.sdase.commons.server.mongo.testing;

import com.mongodb.MongoClient;
import org.junit.rules.ExternalResource;

public class UseExistingMongoDbRule extends ExternalResource implements MongoDbRule {

  private UseExistingMongoDb mongoDb;

  public UseExistingMongoDbRule(String mongoDbConnectionString) {
    mongoDb = new UseExistingMongoDb(mongoDbConnectionString);
  }

  @Override
  protected void before() {
    mongoDb.logConnection();
  }

  @Override
  public String getHosts() {
    return mongoDb.getHosts();
  }

  @Override
  public String getUsername() {
    return mongoDb.getUsername();
  }

  @Override
  public String getPassword() {
    return mongoDb.getPassword();
  }

  @Override
  public String getDatabase() {
    return mongoDb.getDatabase();
  }

  @Override
  public String getOptions() {
    return mongoDb.getOptions();
  }

  @Override
  public MongoClient createClient() {
    return mongoDb.createClient();
  }
}
