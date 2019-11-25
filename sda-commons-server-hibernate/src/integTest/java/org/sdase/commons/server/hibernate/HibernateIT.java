package org.sdase.commons.server.hibernate;

import com.github.database.rider.core.DBUnitRule;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.configuration.DataSetConfig;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.sdase.commons.server.hibernate.test.HibernateApp;
import org.sdase.commons.server.hibernate.test.HibernateITestConfiguration;
import org.sdase.commons.server.hibernate.test.model.Person;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.flywaydb.core.Flyway;
import org.junit.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DBUnit(url = HibernateIT.DB_URI,  driver = "org.h2.Driver", user = "sa", password = "sa")
public class HibernateIT {

   static final String DB_URI = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

   @ClassRule
   public static final DropwizardAppRule<HibernateITestConfiguration> DW =
         new DropwizardAppRule<>(HibernateApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

   @Rule
   public final DBUnitRule dbUnitRule = DBUnitRule.instance();

   @BeforeClass
   public static void initDb() {
      DataSourceFactory database = DW.getConfiguration().getDatabase();
      Flyway flyway = new Flyway(
          new FluentConfiguration().dataSource(database.getUrl(), database.getUser(), database.getPassword())
      );
      flyway.migrate();
   }

   @Before
   public void cleanBefore() throws Exception {
      dbUnitRule.getDataSetExecutor().clearDatabase(new DataSetConfig());
   }

   @Test
   public void shouldAccessEmptyDb() {
      Person[] people = client()
            .path("/api/persons")
            .request(APPLICATION_JSON)
            .get(Person[].class);

      assertThat(people).hasSize(0);
   }

   @Test
   public void shouldWriteInDb() {
      Person entity = new Person();
      entity.setName("John Doe");
      entity.setEmail("j.doe@example.com");
      Response response = client().path("/api/persons").request().post(Entity.json(entity));

      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation().toString()).matches("http.*/api/persons/\\d+");
   }

   @Test
   public void shouldReadFromDb() {
      Person entity = new Person();
      entity.setName("John Doe");
      entity.setEmail("j.doe@example.com");
      client().path("/api/persons").request().post(Entity.json(entity));

      Person[] people = client().path("/api/persons").request(APPLICATION_JSON).get(Person[].class);

      assertThat(people).hasSize(1)
            .extracting(
                  p -> p.getId() > 0,
                  Person::getName,
                  Person::getEmail
            )
            .containsExactly(
                  tuple(true, "John Doe", "j.doe@example.com")
            );
   }

   @Test
   public void shouldAddHibernateHealthCheck() {
      Map<String, HealthCheckResult> healthCheck = DW.client().target("http://localhost:" + DW.getAdminPort())
            .path("/healthcheck")
            .request(APPLICATION_JSON)
            .get(new GenericType<Map<String, HealthCheckResult>>() {});
      assertThat(healthCheck).containsKey("hibernate");
      assertThat(healthCheck.get("hibernate")).extracting(HealthCheckResult::getHealthy).isEqualTo("true");
   }

   private WebTarget client() {
      return DW.client().target("http://localhost:" + DW.getLocalPort());
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
