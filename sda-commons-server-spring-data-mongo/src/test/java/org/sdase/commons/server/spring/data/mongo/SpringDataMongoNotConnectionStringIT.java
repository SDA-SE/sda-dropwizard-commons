package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.mongo.distribution.Version;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.time.LocalDate;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyApp;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.sdase.commons.server.spring.data.mongo.example.model.PhoneNumber;
import org.sdase.commons.server.spring.data.mongo.example.repository.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoOperations;

abstract class SpringDataMongoNotConnectionStringIT {

  static class MongoDb44Test extends SpringDataMongoNotConnectionStringIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            MyApp.class,
            null,
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  static class MongoDb50Test extends SpringDataMongoNotConnectionStringIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            MyApp.class,
            null,
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  @Test
  void shouldStartup() {
    assertThat((MyApp) getDW().getApplication()).isNotNull();
  }

  @Test
  void shouldSaveAndFind() {
    PhoneNumber phoneNumber = new PhoneNumber().setNumber("+49123456789");
    Person person =
        new Person()
            .setAge(44)
            .setName("Mustermann")
            .setBirthday(LocalDate.now().minusYears(44))
            .setPhoneNumber(phoneNumber);

    MyApp app = getDW().getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    Person savedPerson = mongoOperations.save(person);

    Person foundPerson = mongoOperations.findById(savedPerson.getId(), Person.class);
    assertThat(foundPerson).isNotNull();
  }

  @Test
  void readsFirstPageCorrectly() {
    MyApp app = getDW().getApplication();
    PersonRepository repository = app.getPersonRepository();
    Page<Person> persons = repository.findAll(PageRequest.of(0, 10));
    assertThat(persons.isFirst()).isTrue();
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();
}
