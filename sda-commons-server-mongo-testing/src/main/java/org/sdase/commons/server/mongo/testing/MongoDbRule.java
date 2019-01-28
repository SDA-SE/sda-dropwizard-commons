package org.sdase.commons.server.mongo.testing;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.PRODUCTION;
import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.junit.rules.ExternalResource;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ServerClosedEvent;
import com.mongodb.event.ServerDescriptionChangedEvent;
import com.mongodb.event.ServerListener;
import com.mongodb.event.ServerOpeningEvent;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * JUnit Test rule for running a MongoDB instance alongside the (integration) tests.
 * Can be configured with custom user credentials and database name.
 * Use {@link #getHost()} to retrieve the host to connect to.
 * </p>
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 *     <code>
 *         &#64;ClassRule
 *         public static final MongoDbRule RULE = MongoDbRule
 *               .builder()
 *               .withDatabase("my_db")
 *               .withUsername("my_user")
 *               .withPassword("my_s3cr3t")
 *               .build();
 *     </code>
 * </pre>
 */
public class MongoDbRule extends ExternalResource {
   private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbRule.class);

   // Initialization-on-demand holder idiom
   private static class LazyHolder {
      static final MongodStarter INSTANCE = MongodStarter.getDefaultInstance();
   }

   private static MongodStarter ensureMongodStarter() {
      return LazyHolder.INSTANCE;
   }

   public static Builder builder() {
      return new Builder();
   }

   private final IFeatureAwareVersion version;
   private final long timeoutMs;
   private final String username;
   private final String password;
   private final String database;

   private IMongodConfig mongodConfig;
   private MongodExecutable mongodExecutable;

   private volatile boolean started;

   private MongoDbRule(String username, String password, String database, IFeatureAwareVersion version,
         long timeoutMs) {

      this.version = requireNonNull(version, "version");
      this.username = requireNonNull(username, "username");
      this.password = requireNonNull(password, "password");
      this.database = requireNonNull(database, "database");
      this.timeoutMs = timeoutMs;
   }

   /**
    * Returns the hostname and port that can be used to connect to the database.
    * 
    * @return Hostname with port.
    */
   public String getHost() {
      String hostname = "localhost";
      try {
         hostname = mongodConfig.net().getServerAddress().getHostAddress();
      } catch (UnknownHostException e) {
         LOGGER.error("Unknown host name", e);
      }

      return hostname + ":" + mongodConfig.net().getPort();
   }

   @Override
   protected void before() {
      startMongo();
   }

   @Override
   protected void after() {
      stopMongo();
   }

   private void startMongo() {
      if (started) {
         return;
      }

      try {
         mongodConfig = new MongodConfigBuilder().version(version).withLaunchArgument("--bind_ip", "127.0.0.1").build();

         mongodExecutable = ensureMongodStarter().prepare(mongodConfig);
         mongodExecutable.start();

         final CountDownLatch countDownLatch = new CountDownLatch(1);

         final MongoClientOptions options = MongoClientOptions.builder().addServerListener(new ServerListener() {
            @Override
            public void serverOpening(final ServerOpeningEvent event) {
               countDownLatch.countDown();
            }

            @Override
            public void serverClosed(final ServerClosedEvent event) {
               // no action required
            }

            @Override
            public void serverDescriptionChanged(final ServerDescriptionChangedEvent event) {
               // no action required
            }
         }).build();

         try (MongoClient mongoClient = new MongoClient(getHost(), options)) {
            // ensure MongoDB is available before proceeding
            if (!countDownLatch.await(timeoutMs, MILLISECONDS)) {
               throw new IllegalStateException("Timeout, MongoDB not started.");
            }

            // Create the database user for the test context
            createDatabaseUser(mongoClient);
         }
      } catch (IOException e) {
         throw new IllegalStateException(e);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }

      started = true;

      // safety net
      getRuntime().addShutdownHook(new Thread(this::stopMongo, "shutdown mongo"));
   }

   private void stopMongo() {
      if (started && mongodExecutable != null) {
         mongodExecutable.stop();
         started = false;
      }
   }

   private void createDatabaseUser(MongoClient mongoClient) {
      MongoDatabase db = mongoClient.getDatabase(database);

      final BasicDBObject createUserCommand = new BasicDBObject("createUser", username)
            .append("pwd", password)
            .append("roles", Collections.singletonList(new BasicDBObject("role", "readWrite").append("db", database)));
      db.runCommand(createUserCommand);
   }

   public static final class Builder {

      private static final Main DEFAULT_VERSION = PRODUCTION;

      private static final long DEFAULT_TIMEOUT_MS = MINUTES.toMillis(1L);

      public static final String DEFAULT_USER = "dbuser";
      public static final String DEFAULT_PASSWORD = "sda123"; // NOSONAR
      public static final String DEFAULT_DATABASE = "default_db";

      private IFeatureAwareVersion version;
      private Long timeoutInMillis;
      private String username = DEFAULT_USER;
      private String password = DEFAULT_PASSWORD; // NOSONAR
      private String database = DEFAULT_DATABASE;

      private Builder() {
         // prevent instantiation
      }

      /**
       * Configure the username that can be used to connect to the MongoDB
       * instance, the default user is "dbuser" ({@link #DEFAULT_USER}).
       */
      public Builder withUsername(String username) {
         this.username = username;
         return this;
      }

      /**
       * Configure the password that can be used to connect to the MongoDB
       * instance, the default password is "sda123" ({@link #DEFAULT_PASSWORD}).
       */
      public Builder withPassword(String password) {
         this.password = password;
         return this;
      }

      /**
       * Configure the database that can be used to connect to the MongoDB
       * instance, the default database is "default_db"
       * ({@link #DEFAULT_DATABASE}).
       */
      public Builder withDatabase(String database) {
         this.database = database;
         return this;
      }

      /**
       * Configure the MongoDB version to start, by default the latest
       * production version is used ({@link #DEFAULT_VERSION}).
       */
      public Builder withVersion(IFeatureAwareVersion version) {
         this.version = version;
         return this;
      }

      /**
       * Configures the timeout for database startup, the default value is one
       * minute ({@link #DEFAULT_TIMEOUT_MS}).
       */
      public Builder withTimeoutInMillis(long timeoutInMillis) {
         this.timeoutInMillis = timeoutInMillis;
         return this;
      }

      public MongoDbRule build() {
         IFeatureAwareVersion v = version == null ? DEFAULT_VERSION : version;
         long t = timeoutInMillis == null || timeoutInMillis < 1L ? DEFAULT_TIMEOUT_MS : timeoutInMillis;

         return new MongoDbRule(username, password, database, v, t);
      }
   }
}
