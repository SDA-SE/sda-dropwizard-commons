package org.sdase.commons.server.spring.data.mongo.metrics;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyApp;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.sdase.commons.server.spring.data.mongo.example.model.PhoneNumber;
import org.springframework.data.mongodb.core.MongoOperations;

abstract class SpringDataMongoMetricsIT {

  static class MongoDb44Test extends SpringDataMongoMetricsIT {
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

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  static class MongoDb50Test extends SpringDataMongoMetricsIT {
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

    String telephoneNumber = "+49123456789";
    int age = 44;
    String name = "Mustermann";

    PhoneNumber phoneNumber = new PhoneNumber().setNumber(telephoneNumber);
    Person person =
        new Person()
            .setAge(age)
            .setName(name)
            .setBirthday(LocalDate.now().minusYears(age))
            .setPhoneNumber(phoneNumber);

    MyApp app = getDW().getApplication();
    MongoOperations mongoOperations = app.getMongoOperations();
    Person savedPerson = mongoOperations.save(person);
    mongoOperations.findById(savedPerson.getId(), Person.class);

    //    Test that metrics are available in global registry
    List<Meter> meters = Metrics.globalRegistry.getMeters();
    List<String> metricIdNames =
        meters.stream().map(meter -> meter.getId().getName()).collect(Collectors.toList());
    assertThat(metricIdNames)
        .containsAll(
            List.of(
                "mongodb.driver.commands",
                "mongodb.driver.pool.size",
                "mongodb.driver.pool.waitqueuesize",
                "mongodb.driver.pool.checkedout"));

    //    Test no personal data is exposed through metrics
    Optional<Meter> commandsCompositeTime =
        meters.stream()
            .filter(meter -> meter.getId().getName().equals("mongodb.driver.commands"))
            .findFirst();
    assertThat(commandsCompositeTime).isPresent();
    var tagsWithoutClusterId =
        commandsCompositeTime.get().getId().getTags().stream()
            .filter(t -> !"cluster.id".equals(t.getKey()))
            .filter(t -> !"server.address".equals(t.getKey()))
            .collect(Collectors.toList());
    assertThat(tagsWithoutClusterId.toString())
        .doesNotContain(List.of(telephoneNumber, Integer.toString(age), name));
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();
}
