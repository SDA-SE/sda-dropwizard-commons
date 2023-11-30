package org.sdase.commons.server.spring.data.mongo;

import static de.flapdoodle.embed.mongo.distribution.Version.Main.V4_4;
import static de.flapdoodle.embed.mongo.distribution.Version.Main.V5_0;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.GenericType;
import java.util.Map;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyApp;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;

abstract class SpringDataMongoBundleHealthCheckIT {

  static class MongoDb44Test extends SpringDataMongoBundleHealthCheckIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            MyApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  static class MongoDb50Test extends SpringDataMongoBundleHealthCheckIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            MyApp.class,
            null,
            randomPorts(),
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  @Test
  void shouldRegisterHealthCheck() {
    String healthcheckName = "mongo";
    Map<String, HealthCheckResult> healthCheck =
        getDW()
            .client()
            .target("http://localhost:" + getDW().getAdminPort())
            .path("/healthcheck")
            .request(APPLICATION_JSON)
            .get(new GenericType<>() {});
    assertThat(healthCheck).containsKey(healthcheckName);
    assertThat(healthCheck.get(healthcheckName))
        .extracting(HealthCheckResult::getHealthy)
        .isEqualTo("true");
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();

  static class HealthCheckResult {
    private String healthy;

    String getHealthy() {
      return healthy;
    }

    public HealthCheckResult setHealthy(String healthy) {
      this.healthy = healthy;
      return this;
    }
  }
}
