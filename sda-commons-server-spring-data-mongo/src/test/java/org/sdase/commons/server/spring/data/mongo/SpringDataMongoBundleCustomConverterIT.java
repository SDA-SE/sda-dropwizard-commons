package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.distribution.Version;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.sdase.commons.server.spring.data.mongo.example.model.PhoneNumber;
import org.sdase.commons.server.spring.data.mongo.example.model.PhoneNumberToStringConverter;
import org.sdase.commons.server.spring.data.mongo.example.model.StringToPhoneNumberConverter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

abstract class SpringDataMongoBundleCustomConverterIT {

  static class MongoDb44Test extends SpringDataMongoBundleCustomConverterIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            TestApp.class,
            null,
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  static class MongoDb50Test extends SpringDataMongoBundleCustomConverterIT {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension mongo =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<MyConfiguration> DW =
        new DropwizardAppExtension<>(
            TestApp.class,
            null,
            config("springDataMongo.connectionString", mongo::getConnectionString));

    @Override
    DropwizardAppExtension<MyConfiguration> getDW() {
      return DW;
    }
  }

  @BeforeEach
  void verifyIndexBeforeAccessAndClean() {
    getMongoOperations().findAllAndRemove(new Query(), Person.class);
  }

  @Test
  void writeCustomPhoneNumber() {
    MongoOperations mongoOperations = getMongoOperations();
    PhoneNumber phoneNumber =
        new PhoneNumber().setCountryCode("0049").setAreaCode("0172").setNumber("123 456 789");
    Person johnDoe =
        mongoOperations.save(
            new Person().setName("John Doe").setAge(42).setPhoneNumber(phoneNumber));

    MongoCollection<Document> peopleCollection = mongoOperations.getCollection("people");
    List<String> phoneNumbers = new ArrayList<>();
    peopleCollection.find().forEach(d -> phoneNumbers.add(d.get("phoneNumber").toString()));
    assertThat(phoneNumbers).containsExactly("+49 172 123456789");

    Person johnDoeFromMongoOperations =
        mongoOperations.findAll(Person.class).stream()
            .filter(p -> p.getId().equals(johnDoe.getId()))
            .findFirst()
            .orElse(null);
    assertThat(johnDoeFromMongoOperations).isNotNull();
    assertThat(johnDoeFromMongoOperations.getPhoneNumber())
        .extracting(PhoneNumber::getCountryCode, PhoneNumber::getAreaCode, PhoneNumber::getNumber)
        .containsExactly("+49", "172", "123456789");
  }

  private MongoOperations getMongoOperations() {
    return getDW().<TestApp>getApplication().getMongoOperations();
  }

  public static class TestApp extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(Person.class)
            .addCustomConverters(
                new PhoneNumberToStringConverter(), new StringToPhoneNumberConverter())
            .build();

    @Override
    public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      bootstrap.addBundle(springDataMongoBundle);
    }

    @Override
    public void run(MyConfiguration configuration, Environment environment) {
      // nothing to run
    }

    public MongoOperations getMongoOperations() {
      return springDataMongoBundle.getMongoOperations();
    }
  }

  abstract DropwizardAppExtension<MyConfiguration> getDW();
}
