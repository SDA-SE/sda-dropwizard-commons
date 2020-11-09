package org.sdase.commons.server.hibernate.example.test;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.database.rider.core.DBUnitRule;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.configuration.DataSetConfig;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.sql.SQLException;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.hibernate.example.HibernateExampleApplication;
import org.sdase.commons.server.hibernate.example.HibernateExampleConfiguration;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;

@DBUnit(
    url = HibernateExampleIT.DB_URI,
    driver = "org.h2.Driver",
    user = "sa",
    password = "sa") // NOSONAR
public class HibernateExampleIT {

  static final String DB_URI = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

  @ClassRule
  public static final DropwizardAppRule<HibernateExampleConfiguration> DW =
      new DropwizardAppRule<>(
          HibernateExampleApplication.class,
          resourceFilePath("test-config.yaml"),
          config("database.driverClass", "org.h2.Driver"),
          config("database.user", "sa"),
          config("database.password", "sa"),
          config("database.url", DB_URI));

  @ClassRule public static final DBUnitRule dbUnitRule = DBUnitRule.instance();

  @BeforeClass
  public static void initDb() {
    DataSourceFactory database = DW.getConfiguration().getDatabase();
    Flyway flyway =
        new Flyway(
            new FluentConfiguration()
                .dataSource(database.getUrl(), database.getUser(), database.getPassword()));
    flyway.migrate();
  }

  @Before
  public void cleanBefore() throws SQLException {
    dbUnitRule.getDataSetExecutor().clearDatabase(new DataSetConfig());
  }

  @Test
  public void shouldWriteAndReadPerson() {
    String name = "Json Borne";
    PersonEntity person = new PersonEntity();
    person.setName(name);

    WebTarget persons = DW.client().target("http://localhost:" + DW.getLocalPort()).path("persons");

    Response postResponse = persons.request(APPLICATION_JSON).post(Entity.json(person));
    String personLocation = postResponse.getHeaderString("Location");
    assertThat(personLocation).isNotEmpty();

    PersonEntity storedPerson =
        DW.client().target(personLocation).request(APPLICATION_JSON).get(PersonEntity.class);
    assertThat(storedPerson).isEqualToIgnoringGivenFields(person, "id");
  }

  @Test
  public void shouldAddHibernateHealthCheck() {
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
