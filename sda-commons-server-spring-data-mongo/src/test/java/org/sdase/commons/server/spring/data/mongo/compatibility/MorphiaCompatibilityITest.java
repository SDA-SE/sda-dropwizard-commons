package org.sdase.commons.server.spring.data.mongo.compatibility;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.compatibility.app.CompatibilityTestApp;
import org.sdase.commons.server.spring.data.mongo.compatibility.app.CompatibilityTestApp.MyEntityWithGenericsRepository;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.GenericIntType;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.GenericStringType;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEntity;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEntityWithGenerics;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEnum;
import org.springframework.data.mongodb.core.MongoOperations;

class MorphiaCompatibilityITest {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension MONGO = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<CompatibilityTestApp.Config> DW =
      new DropwizardAppExtension<>(
          CompatibilityTestApp.class,
          null,
          config("springDataMongo.connectionString", MONGO::getConnectionString));

  MongoOperations mongoOperations;
  MyEntityWithGenericsRepository myEntityWithGenericsRepository;

  @BeforeEach
  void cleanCollection() {
    MONGO.clearCollections();
  }

  @BeforeEach
  void initMongoOperations() {
    CompatibilityTestApp app = DW.getApplication();
    this.mongoOperations = app.getMongoOperations();
    this.myEntityWithGenericsRepository = app.getMyEntityWithGenericsRepository();
  }

  @Test
  void shouldStoreGenericTypes() {
    String id = UUID.randomUUID().toString();
    var given =
        new MyEntityWithGenerics().setId(id).setGenericValue(new GenericIntType().setValue(123));

    myEntityWithGenericsRepository.save(given);

    Optional<MyEntityWithGenerics> actual = myEntityWithGenericsRepository.findById(id);
    assertThat(actual)
        .isPresent()
        .get()
        .extracting(MyEntityWithGenerics::getId, it -> it.getGenericValue().getValue())
        .contains(id, 123);
    assertThat(actual.get().getGenericValue()).isExactlyInstanceOf(GenericIntType.class);
  }

  @Test
  @Disabled(
      "This test shows the default of Spring Data MongoDB - using _class instead of Morphia's className.")
  void verifyHowGenericTypesAreStoredBySpringData() {
    String id = UUID.randomUUID().toString();
    var given =
        new MyEntityWithGenerics().setId(id).setGenericValue(new GenericIntType().setValue(123));

    myEntityWithGenericsRepository.save(given);

    var actual =
        mongoOperations
            .getCollection("MyEntityWithGenerics")
            .find(new BsonDocument("_id", new BsonString(id)), Map.class);
    assertThat(actual)
        .hasSize(1)
        .extracting(Map::keySet)
        .containsExactlyInAnyOrder(Set.of("_id", "genericValue", "_class"));
    //noinspection unchecked
    assertThat(actual.first())
        .extracting("genericValue")
        .extracting("_class", "value")
        .contains(
            "org.sdase.commons.server.spring.data.mongo.compatibility.model.GenericIntType", 123);
  }

  @Test
  void shouldReadGenericsWithClassnameStoredWithMongoOperations() {
    insertTestDataFromResource("MyEntityWithGenerics", "MyEntityWithGenerics.classname.json");

    var actual =
        mongoOperations.findById(
            "1a736daf-6ff0-4779-9ace-f8eef956739e", MyEntityWithGenerics.class);

    assertThat(actual)
        .isNotNull()
        .extracting(MyEntityWithGenerics::getId, it -> it.getGenericValue().getValue())
        .contains("1a736daf-6ff0-4779-9ace-f8eef956739e", "a string");
    assertThat(actual.getGenericValue()).isExactlyInstanceOf(GenericStringType.class);
  }

  @Test
  void shouldReadWithClassnameStoredWithMongoOperations() {
    insertTestDataFromResource("MyEntity", "MyEntity.classname.json");

    var actual = mongoOperations.findById("1a736daf-6ff0-4779-9ace-f8eef956739e", MyEntity.class);

    assertThat(actual)
        .isNotNull()
        .extracting(
            MyEntity::getId,
            MyEntity::getStringValue,
            MyEntity::getCharValue,
            MyEntity::getEnumValue,
            MyEntity::getCharArrayValue,
            MyEntity::getUriValue,
            MyEntity::getCharArrayValue,
            MyEntity::getLocaleValue,
            MyEntity::getCurrencyValue,
            MyEntity::getBigDecimalValue,
            MyEntity::getDateValue,
            MyEntity::getInstantValue,
            MyEntity::getLocalDateTimeValue)
        .contains(
            "1a736daf-6ff0-4779-9ace-f8eef956739e",
            "a string",
            'a',
            MyEnum.C,
            "a char array".toCharArray(),
            URI.create("http://foo.bar.test/hello-world/index.html"),
            Locale.GERMANY,
            Currency.getInstance("XAU"),
            new BigDecimal("123.45"),
            Date.from(Instant.parse("2022-10-10T08:10:21.130Z")),
            Instant.parse("2022-10-10T08:10:21.130Z"),
            LocalDateTime.parse("2022-10-10T10:10:21.130"));
  }

  @Test
  void shouldReadAllWithClassnameStoredWithMongoOperations() {
    insertTestDataFromResource("MyEntity", "MyEntity.classname.json");

    var actual = mongoOperations.findAll(MyEntity.class);

    assertThat(actual)
        .hasSize(1)
        .extracting(MyEntity::getId, MyEntity::getStringValue)
        .contains(Tuple.tuple("1a736daf-6ff0-4779-9ace-f8eef956739e", "a string"));
  }

  private void insertTestDataFromResource(String collection, String jsonResourcePath) {
    try (var jsonSource = getClass().getResourceAsStream(jsonResourcePath)) {
      var json = new String(requireNonNull(jsonSource).readAllBytes(), StandardCharsets.UTF_8);
      var document = Document.parse(json);
      mongoOperations.getCollection(collection).insertOne(document);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
