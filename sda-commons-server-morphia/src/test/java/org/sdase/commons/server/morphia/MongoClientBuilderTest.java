package org.sdase.commons.server.morphia;

import static org.junit.Assert.assertEquals;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import io.dropwizard.setup.Environment;
import org.junit.Test;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import org.mockito.Mockito;

public class MongoClientBuilderTest {
/*

   @Test
   public void testMongoClientWithCert() {

      MongoConfiguration mongoConfiguration = setUpMongoConfiguration();
      mongoConfiguration.setCertificate("VEVTVC5DRVJUSUZJQ0FURQo=");

      MongoClientBuilder client = new MongoClientBuilder();
      client.withConfiguration(mongoConfiguration);
      MongoClient mongoClient = client
          .build(Mockito.mock(Environment.class, Mockito.RETURNS_DEEP_STUBS));

      assertEquals(2, mongoClient.getDatabase(mongoConfiguration.getDatabase()));
      assertEquals("db1.example.net", mongoClient.getAllAddress().get(0).getHost());
      assertEquals(27017, mongoClient.getAllAddress().get(0).getPort());
   }

   @Test
   public void testMongoClient() {
      MongoClientBuilder client = new MongoClientBuilder();
      client.withConfiguration(setUpMongoConfiguration());
      Environment.setEnv("SECRET_MONGODB_CA_CERTIFICATE_BASE64", "");

      MongoClient mongoClient = client.build();

      assertEquals(2, mongoClient.getDatabase().size());
      assertEquals("db1.example.net", mongoClient.getAllAddress().get(0).getHost());
      assertEquals(27017, mongoClient.getAllAddress().get(0).getPort());
   }

   @Test(expected = MongoException.class)
   public void testMongoClientSetUpFailure() {
      MongoClientBuilder client = new MongoClientBuilder();
      MongoConfiguration mongoConfiguration = setUpMongoConfiguration();
      mongoConfiguration.setHosts("db1.example.net:27017,db2.example.net:2500:2500");
      client.withConfiguration(mongoConfiguration);
      Environment.setEnv("SECRET_MONGODB_CA_CERTIFICATE_BASE64", "");

      client.build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testConfigurationNull() {
      MongoClientBuilder client = new MongoClientBuilder();
      client.withConfiguration(null);

      client.build();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testDefaultConfigurationNull() {
      MongoClientBuilder client = new MongoClientBuilder();
      client.withConfiguration(setUpMongoConfiguration());
      client.withDefaultOptions(null);

      client.build();
   }

   @Test
   public void testUriBuilderUserPassword() {
      MongoClientBuilder client = new MongoClientBuilder();
      client.withConfiguration(setUpMongoConfiguration());

      String uri = client.buildMongoDbUri();

      assertEquals(
          "mongodb://dbuser:sda123@db1.example.net:27017,db2.example.net:2500/default_db?replicaSet=test",
          uri);
   }

   @Test
   public void testUriBuilderWithoutUserPassword() {
      MongoClientBuilder client = new MongoClientBuilder();
      MongoConfiguration mongoConfiguration = setUpMongoConfiguration();
      mongoConfiguration.setUsername(null);
      mongoConfiguration.setPassword(null);
      client.withConfiguration(mongoConfiguration);

      String uri = client.buildMongoDbUri();

      assertEquals(
          "mongodb://db1.example.net:27017,db2.example.net:2500/default_db?replicaSet=test", uri);
   }

   @Test
   public void testUriBuilderWithoutOptions() {
      MongoClientBuilder client = new MongoClientBuilder();
      MongoConfiguration mongoConfiguration = setUpMongoConfiguration();
      mongoConfiguration.setOptions(null);
      client.withConfiguration(mongoConfiguration);

      String uri = client.buildMongoDbUri();

      assertEquals("mongodb://dbuser:sda123@db1.example.net:27017,db2.example.net:2500/default_db",
          uri);
   }

   @Test
   public void testIsConfigured() {
      MongoClientBuilder client = new MongoClientBuilder();

      boolean checkEnv = client.isConfigured("db1.example.net:27017,db2.example.net");

      assertEquals(true, checkEnv);
   }

   @Test
   public void testIsConfiguredCheckNull() {
      MongoClientBuilder client = new MongoClientBuilder();

      boolean checkEnv = client.isConfigured(null);

      assertEquals(false, checkEnv);
   }

   @Test
   public void testIsConfiguredCheckEmpty() {
      MongoClientBuilder client = new MongoClientBuilder();

      boolean checkEnv = client.isConfigured("");

      assertEquals(false, checkEnv);
   }

   @Test
   public void testIsConfiguredCheckBegin() {
      MongoClientBuilder client = new MongoClientBuilder();

      boolean checkEnv = client.isConfigured("${DB_HOST}");

      assertEquals(false, checkEnv);
   }

   private static MongoConfiguration setUpMongoConfiguration() {

      MongoConfiguration mongoConfiguration = new MongoConfiguration();

      mongoConfiguration.setDatabase("default_db");
      mongoConfiguration.setHosts("db1.example.net:27017,db2.example.net:2500");
      mongoConfiguration.setOptions("replicaSet=test");
      mongoConfiguration.setUsername("dbuser");
      mongoConfiguration.setPassword("sda123");

      return mongoConfiguration;
   }*/
}