package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.time.LocalDate;
import java.util.Optional;
import javax.validation.ConstraintViolationException;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyAppWithValidation;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.sdase.commons.server.spring.data.mongo.example.model.PhoneNumber;
import org.sdase.commons.server.spring.data.mongo.example.repository.PersonRepository;
import org.springframework.data.mongodb.core.MongoOperations;

class SpringDataMongoWithValidationIT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension mongo = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyConfiguration> DW =
      new DropwizardAppExtension<>(
          MyAppWithValidation.class,
          null,
          config("springDataMongo.connectionString", mongo::getConnectionString));

  @Test
  void shouldStartup() {
    assertThat((MyAppWithValidation) DW.getApplication()).isNotNull();
  }

  @Test
  void shouldValidateNameAndAge() {
    MyAppWithValidation app = DW.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    var person = createPerson(0, null);
    var validationError =
        Assertions.assertThrows(
            ConstraintViolationException.class, () -> mongoOperations.save(person));

    assertThat(validationError.getConstraintViolations()).hasSize(2);
    // using filter because the order is not guaranteed in the Set
    assertThat(
            validationError.getConstraintViolations().stream()
                .filter(
                    v ->
                        ((PathImpl) v.getPropertyPath()).asString().equals("name")
                            && v.getMessageTemplate()
                                .equals("{javax.validation.constraints.NotNull.message}"))
                .findFirst())
        .isPresent();
    assertThat(
            validationError.getConstraintViolations().stream()
                .filter(
                    v ->
                        ((PathImpl) v.getPropertyPath()).asString().equals("age")
                            && v.getMessageTemplate()
                                .equals("{javax.validation.constraints.Min.message}"))
                .findFirst())
        .isPresent();
  }

  @Test
  void shouldValidateNameAndAgeWithRepository() {
    MyAppWithValidation app = DW.getApplication();
    PersonRepository repository = app.getPersonRepository();
    var person = createPerson(0, null);
    var validationError =
        Assertions.assertThrows(ConstraintViolationException.class, () -> repository.save(person));
    assertThat(validationError.getConstraintViolations()).hasSize(2);
    // using filter because the order is not guaranteed in the Set
    assertThat(
            validationError.getConstraintViolations().stream()
                .filter(
                    v ->
                        ((PathImpl) v.getPropertyPath()).asString().equals("name")
                            && v.getMessageTemplate()
                                .equals("{javax.validation.constraints.NotNull.message}"))
                .findFirst())
        .isPresent();
    assertThat(
            validationError.getConstraintViolations().stream()
                .filter(
                    v ->
                        ((PathImpl) v.getPropertyPath()).asString().equals("age")
                            && v.getMessageTemplate()
                                .equals("{javax.validation.constraints.Min.message}"))
                .findFirst())
        .isPresent();
  }

  @Test
  void shouldSaveAndFind() {
    MyAppWithValidation app = DW.getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    Person savedPerson = mongoOperations.save(createPerson(44, "Mustermann"));

    Person foundPerson = mongoOperations.findById(savedPerson.getId(), Person.class);
    assertThat(foundPerson).isNotNull();
  }

  @Test
  void shouldSaveAndFindWithRepository() {
    MyAppWithValidation app = DW.getApplication();
    PersonRepository repository = app.getPersonRepository();
    Person savedPerson = repository.save(createPerson(44, "Mustermann"));

    Optional<Person> foundPerson = repository.findById(savedPerson.getId());
    assertThat(foundPerson).isPresent();
  }

  private Person createPerson(int age, String name) {
    PhoneNumber phoneNumber = new PhoneNumber().setNumber("+49123456789");
    return new Person()
        .setAge(age)
        .setName(name)
        .setBirthday(LocalDate.now().minusYears(44))
        .setPhoneNumber(phoneNumber);
  }
}
