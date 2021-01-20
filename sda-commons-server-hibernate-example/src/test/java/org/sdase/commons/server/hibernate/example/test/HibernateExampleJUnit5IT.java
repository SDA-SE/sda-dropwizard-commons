package org.sdase.commons.server.hibernate.example.test;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.junit5.DBUnitExtension;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.hibernate.example.HibernateExampleApplication;
import org.sdase.commons.server.hibernate.example.HibernateExampleConfiguration;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;

@DBUnit(
    url = HibernateExampleJUnit5IT.DB_URI,
    driver = "org.h2.Driver",
    user = "sa",
    password = "sa") // NOSONAR
@ExtendWith(DBUnitExtension.class)
class HibernateExampleJUnit5IT {

  static final String DB_URI = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

  @RegisterExtension
  static final DropwizardAppExtension<HibernateExampleConfiguration> DW =
      new DropwizardAppExtension<>(
          HibernateExampleApplication.class,
          null,
          randomPorts(),
          config("database.driverClass", "org.h2.Driver"),
          config("database.user", "sa"),
          config("database.password", "sa"),
          config("database.url", DB_URI));

  @BeforeAll
  public static void initDb() {
    DataSourceFactory database = DW.getConfiguration().getDatabase();
    Flyway flyway =
        new Flyway(
            new FluentConfiguration()
                .dataSource(database.getUrl(), database.getUser(), database.getPassword()));
    flyway.migrate();
  }

  @Test
  void shouldWriteAndReadPerson() {
    String name = "Json Borne";
    PersonEntity person = new PersonEntity();
    person.setName(name);

    WebTarget persons = DW.client().target("http://localhost:" + DW.getLocalPort()).path("persons");

    Response postResponse = persons.request(APPLICATION_JSON).post(Entity.json(person));
    String personLocation = postResponse.getHeaderString("Location");
    assertThat(personLocation).isNotEmpty();

    PersonEntity storedPerson =
        DW.client().target(personLocation).request(APPLICATION_JSON).get(PersonEntity.class);
    assertThat(storedPerson).usingRecursiveComparison().ignoringFields("id").isEqualTo(person);
  }

  @Test
  void shouldAddHibernateHealthCheck() {
    Map<String, HealthCheckResult> healthCheck =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("healthcheck")
            .request(APPLICATION_JSON)
            .get(new GenericType<Map<String, HealthCheckResult>>() {});
    assertThat(healthCheck).containsKey("hibernate");
    assertThat(healthCheck.get("hibernate"))
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
