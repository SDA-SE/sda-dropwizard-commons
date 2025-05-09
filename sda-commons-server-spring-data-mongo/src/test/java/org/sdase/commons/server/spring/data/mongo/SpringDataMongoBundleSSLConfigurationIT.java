package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.internal.MongoClientImpl;
import de.flapdoodle.embed.mongo.distribution.Version;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.nio.file.Paths;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.springframework.data.mongodb.core.MongoOperations;

abstract class SpringDataMongoBundleSSLConfigurationIT {

  static class MongoDb44Test extends SpringDataMongoBundleSSLConfigurationIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            config(
                "config.customCaCertificateDir", Paths.get("src", "test", "resources").toString()));

    @RegisterExtension
    @Order(2)
    static final DropwizardAppExtension<MyConfiguration> DW_WITHOUT_SSL =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            // make sure certificates dir is unset
            config("config.customCaCertificateDir", ""));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }

    @Override
    DropwizardAppExtension<MyConfiguration> getDwWithoutSSL() {
      return DW_WITHOUT_SSL;
    }
  }

  static class MongoDb50Test extends SpringDataMongoBundleSSLConfigurationIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            config(
                "config.customCaCertificateDir", Paths.get("src", "test", "resources").toString()));

    @RegisterExtension
    @Order(2)
    static final DropwizardAppExtension<MyConfiguration> DW_WITHOUT_SSL =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            // make sure certificates dir is unset
            config("config.customCaCertificateDir", ""));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }

    @Override
    DropwizardAppExtension<MyConfiguration> getDwWithoutSSL() {
      return DW_WITHOUT_SSL;
    }
  }

  static class MongoDb60Test extends SpringDataMongoBundleSSLConfigurationIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V6_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            config(
                "config.customCaCertificateDir", Paths.get("src", "test", "resources").toString()));

    @RegisterExtension
    @Order(2)
    static final DropwizardAppExtension<MyConfiguration> DW_WITHOUT_SSL =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            // make sure certificates dir is unset
            config("config.customCaCertificateDir", ""));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }

    @Override
    DropwizardAppExtension<MyConfiguration> getDwWithoutSSL() {
      return DW_WITHOUT_SSL;
    }
  }

  static class MongoDb70Test extends SpringDataMongoBundleSSLConfigurationIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V7_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            config(
                "config.customCaCertificateDir", Paths.get("src", "test", "resources").toString()));

    @RegisterExtension
    @Order(2)
    static final DropwizardAppExtension<MyConfiguration> DW_WITHOUT_SSL =
        new DropwizardAppExtension<>(
            AutoIndexDisabledApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString),
            // make sure certificates dir is unset
            config("config.customCaCertificateDir", ""));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }

    @Override
    DropwizardAppExtension<MyConfiguration> getDwWithoutSSL() {
      return DW_WITHOUT_SSL;
    }
  }

  @Test
  void shouldHaveCreatedSSLContext() {
    var sslContext = getSSLContext(getDW());
    assertThat(sslContext).isNotNull();
  }

  @Test
  void shouldHaveNotCreatedSSLContext() {
    var sslContext = getSSLContext(getDwWithoutSSL());
    assertThat(sslContext).isNull();
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();

  abstract DropwizardAppExtension<MyConfiguration> getDwWithoutSSL();

  private SSLContext getSSLContext(DropwizardAppExtension<MyConfiguration> DW) {
    return ((MongoClientImpl)
            DW.<AutoIndexDisabledApp>getApplication().springDataMongoBundle.mongoClient())
        .getSettings()
        .getSslSettings()
        .getContext();
  }

  public static class AutoIndexDisabledApp extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .disableAutoIndexCreation()
            .withCaCertificateConfigProvider(MyConfiguration::getConfig)
            .build();

    @Override
    public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      bootstrap.addBundle(springDataMongoBundle);
    }

    @Override
    public void run(MyConfiguration configuration, Environment environment) {
      // nothing to run
    }

    public MongoOperations getMongoOperations() {
      return springDataMongoBundle.getMongoOperations();
    }
  }
}
