package org.sdase.commons.server.hibernate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.ResourceHelpers;
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
import org.sdase.commons.server.hibernate.test.HibernateITestConfiguration;
import org.sdase.commons.server.hibernate.test.HibernatePackageScanApp;
import org.sdase.commons.server.hibernate.test.model.Person;

@DBUnit(url = HibernatePackageScanIT.DB_URI, driver = "org.h2.Driver", user = "sa", password = "sa")
@ExtendWith(DBUnitExtension.class)
@DataSet(cleanBefore = true)
class HibernatePackageScanIT {

  static final String DB_URI = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

  @RegisterExtension
  public static final DropwizardAppExtension<HibernateITestConfiguration> DW =
      new DropwizardAppExtension<>(
          HibernatePackageScanApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @BeforeAll
  static void initDb() {
    DataSourceFactory database = DW.getConfiguration().getDatabase();
    Flyway flyway =
        new Flyway(
            new FluentConfiguration()
                .dataSource(database.getUrl(), database.getUser(), database.getPassword()));
    flyway.migrate();
  }

  @Test
  void shouldAccessEmptyDb() {
    Person[] people = client().path("/api/persons").request(APPLICATION_JSON).get(Person[].class);

    assertThat(people).isEmpty();
  }

  @Test
  void shouldWriteInDb() {
    Person entity = new Person();
    entity.setName("John Doe");
    entity.setEmail("j.doe@example.com");
    Response response = client().path("/api/persons").request().post(Entity.json(entity));

    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(response.getLocation().toString()).matches("http.*/api/persons/\\d+");
  }

  @Test
  void shouldReadFromDb() {
    Person entity = new Person();
    entity.setName("John Doe");
    entity.setEmail("j.doe@example.com");
    client().path("/api/persons").request().post(Entity.json(entity));

    Person[] people = client().path("/api/persons").request(APPLICATION_JSON).get(Person[].class);

    assertThat(people)
        .hasSize(1)
        .extracting(p -> p.getId() > 0, Person::getName, Person::getEmail)
        .containsExactly(tuple(true, "John Doe", "j.doe@example.com"));
  }

  @Test
  void shouldAddHibernateHealthCheck() {
    Map<String, HealthCheckResult> healthCheck =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort())
            .path("/healthcheck")
            .request(APPLICATION_JSON)
            .get(new GenericType<Map<String, HealthCheckResult>>() {});
    assertThat(healthCheck).containsKey("hibernate");
    assertThat(healthCheck.get("hibernate"))
        .extracting(HealthCheckResult::getHealthy)
        .isEqualTo("true");
  }

  private WebTarget client() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }

  static class HealthCheckResult {
    private String healthy;

    String getHealthy() {
      return healthy;
    }

    @SuppressWarnings("unused")
    public HealthCheckResult setHealthy(String healthy) {
      this.healthy = healthy;
      return this;
    }
  }
}
