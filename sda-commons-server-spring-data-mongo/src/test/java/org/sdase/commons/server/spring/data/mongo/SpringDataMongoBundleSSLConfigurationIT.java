package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.internal.MongoClientImpl;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.nio.file.Paths;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.springframework.data.mongodb.core.MongoOperations;

class SpringDataMongoBundleSSLConfigurationIT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension mongo = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyConfiguration> DW =
      new DropwizardAppExtension<>(
          AutoIndexDisabledApp.class,
          null,
          randomPorts(),
          config("springDataMongo.connectionString", mongo::getConnectionString),
          config("springDataMongo.useSsl", "true"),
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
          config("springDataMongo.useSsl", "false"),
          config(
              "config.customCaCertificateDir", Paths.get("src", "test", "resources").toString()));

  @Test
  void shouldHaveCreatedSSLContext() {
    var sslContext = getSSLContext(DW);
    assertThat(sslContext).isNotNull();
  }

  @Test
  void shouldHaveNotCreatedSSLContext() {
    var sslContext = getSSLContext(DW_WITHOUT_SSL);
    assertThat(sslContext).isNull();
  }

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
