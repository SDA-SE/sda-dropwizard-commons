package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.mongodb.MongoClient;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyApp;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/** Tests if entities can be added by exact definition. */
class MorphiaBundleLocalDateConvertersIT {

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

  @BeforeEach
  void cleanCollection() {
    mongo.clearCollections();
  }

  @Test
  void supportLocalDateAndLocalDateTime() {
    LocalDate birthday = LocalDate.of(1979, 2, 8);
    LocalDateTime lastLogin = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    ZonedDateTime zonedDateTime = ZonedDateTime.now();
    MongoOperations mongoOperations = getMongoOperations();
    mongoOperations.save(
        new Person()
            .setName("Peter Parker")
            .setBirthday(birthday)
            .setLastLogin(lastLogin)
            .setZonedDateTime(zonedDateTime));

    Query query = new Query();
    query.addCriteria(Criteria.where("name").is("Peter Parker"));
    Person foundPerson = mongoOperations.findOne(query, Person.class);

    assertThat(foundPerson).isNotNull();
    assertThat(foundPerson.getName()).isEqualTo("Peter Parker");
    assertThat(foundPerson.getBirthday()).isEqualTo(birthday);
    assertThat(foundPerson.getLastLogin()).isEqualTo(lastLogin);
    assertThat(foundPerson.getZonedDateTime())
        .isCloseTo(zonedDateTime, within(1, ChronoUnit.MILLIS));
  }

  @Test
  void supportLocalDateRaw() {
    LocalDate birthday = LocalDate.of(1979, 2, 8);
    getMongoOperations().save(new Person().setName("Peter Parker").setBirthday(birthday));

    try (MongoClient client = mongo.createClient()) {
      Document foundPerson =
          client.getDatabase(mongo.getDatabase()).getCollection("people").find().first();

      assertThat(foundPerson).isNotNull();
      assertThat(foundPerson.get("birthday")).isInstanceOf(String.class).isEqualTo("1979-02-08");
      assertThat(foundPerson.get("name")).isInstanceOf(String.class).isEqualTo("Peter Parker");
    }
  }

  private MongoOperations getMongoOperations() {
    return ((MyApp) DW.getApplication()).getMongoOperations();
  }
}
