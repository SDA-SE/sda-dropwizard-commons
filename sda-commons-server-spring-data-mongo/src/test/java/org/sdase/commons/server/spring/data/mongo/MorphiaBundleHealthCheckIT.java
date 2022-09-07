package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyApp;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;

class MorphiaBundleHealthCheckIT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension mongo = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyConfiguration> DW =
      new DropwizardAppExtension<>(
          MyApp.class,
          null,
          config("springDataMongo.connectionString", mongo::getConnectionString));

  @Test
  void shouldRegisterHealthCheck() {
    String healthcheckName = "mongo";
    Map<String, HealthCheckResult> healthCheck =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("/healthcheck")
            .request(APPLICATION_JSON)
            .get(new GenericType<>() {});
    assertThat(healthCheck).containsKey(healthcheckName);
    assertThat(healthCheck.get(healthcheckName))
        .extracting(HealthCheckResult::getHealthy)
        .isEqualTo("true");
  }

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
