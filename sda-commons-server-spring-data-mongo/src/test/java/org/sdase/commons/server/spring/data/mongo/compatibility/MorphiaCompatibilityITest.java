package org.sdase.commons.server.spring.data.mongo.compatibility;

import static io.dropwizard.testing.ConfigOverride.config;
import static java.time.ZoneId.systemDefault;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.mongo.distribution.Version;
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
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultTimeZone;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.compatibility.app.CompatibilityTestApp;
import org.sdase.commons.server.spring.data.mongo.compatibility.app.CompatibilityTestApp.MyEntityWithGenericsRepository;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.GenericIntType;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.GenericStringType;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEntity;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEntityWithGenerics;
import org.sdase.commons.server.spring.data.mongo.compatibility.model.MyEnum;
import org.springframework.data.mongodb.core.MongoOperations;

abstract class MorphiaCompatibilityITest {

  static class MongoDb44Test extends MorphiaCompatibilityITest {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension MONGO =
        MongoDbClassExtension.builder().withVersion(Version.Main.V4_4).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<CompatibilityTestApp.Config> DW =
        new DropwizardAppExtension<>(
            CompatibilityTestApp.class,
            null,
            config("springDataMongo.connectionString", MONGO::getConnectionString));

    @Override
    DropwizardAppExtension<CompatibilityTestApp.Config> getDW() {
      return DW;
    }

    @Override
    MongoDbClassExtension getMongo() {
      return MONGO;
    }
  }

  static class MongoDb50Test extends MorphiaCompatibilityITest {
    @RegisterExtension
    @Order(0)
    static final MongoDbClassExtension MONGO =
        MongoDbClassExtension.builder().withVersion(Version.Main.V5_0).build();

    @RegisterExtension
    @Order(1)
    static final DropwizardAppExtension<CompatibilityTestApp.Config> DW =
        new DropwizardAppExtension<>(
            CompatibilityTestApp.class,
            null,
            config("springDataMongo.connectionString", MONGO::getConnectionString));

    @Override
    DropwizardAppExtension<CompatibilityTestApp.Config> getDW() {
      return DW;
    }

    @Override
    MongoDbClassExtension getMongo() {
      return MONGO;
    }
  }

  MongoOperations mongoOperations;
  MyEntityWithGenericsRepository myEntityWithGenericsRepository;

  @BeforeEach
  void cleanCollection() {
    getMongo().clearCollections();
  }

  @BeforeEach
  void initMongoOperations() {
    CompatibilityTestApp app = getDW().getApplication();
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

  @ParameterizedTest
  @ValueSource(strings = {"MyEntity.classname.json", "MyEntity.no-classname.json"})
  void shouldReadWithMongoOperations(String givenResource) {
    insertTestDataFromResource("MyEntity", givenResource);

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
            LocalDateTime.ofInstant(Instant.parse("2022-10-10T08:10:21.130Z"), systemDefault()));
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

  @Test
  @DefaultTimeZone("UTC")
  void shouldWriteWithMongoOperationsAsWithMorphia() {

    var given =
        new MyEntity()
            .setId("ID_1234")
            .setBigDecimalValue(new BigDecimal("12.34"))
            .setCharArrayValue(new char[] {'c', 'h'})
            .setCharValue('c')
            .setDateValue(new Date(10L * 1_000))
            .setCurrencyValue(Currency.getInstance("XAU"))
            .setEnumValue(MyEnum.A)
            .setInstantValue(Instant.ofEpochSecond(10L))
            .setLocalDateTimeValue(LocalDateTime.of(2000, 10, 20, 13, 23))
            .setLocaleValue(Locale.GERMANY)
            .setStringValue("a String")
            .setUriValue(URI.create("http://example.com"));

    mongoOperations.insert(given);

    try (var mongoClient = getMongo().createClient()) {
      var actual =
          mongoClient
              .getDatabase(getMongo().getMongoConnectionString().getDatabase())
              .getCollection("MyEntity")
              .find(new BsonDocument("_id", new BsonString("ID_1234")))
              .first();
      assertThat(actual)
          .extracting(
              d -> d.get("_id"),
              d -> d.get("bigDecimalValue"),
              d -> d.get("charArrayValue"),
              d -> d.get("charValue"),
              d -> d.get("dateValue"),
              d -> d.get("currencyValue"),
              d -> d.get("enumValue"),
              d -> d.get("instantValue"),
              d -> d.get("localDateTimeValue"),
              d -> d.get("localeValue"),
              d -> d.get("stringValue"),
              d -> d.get("uriValue"))
          .containsExactly(
              "ID_1234",
              new Decimal128(new BigDecimal("12.34")),
              "ch",
              "c",
              Date.from(Instant.parse("1970-01-01T00:00:10Z")),
              "XAU",
              "A",
              Date.from(Instant.parse("1970-01-01T00:00:10Z")),
              Date.from(Instant.parse("2000-10-20T13:23:00Z")),
              "de_DE",
              "a String",
              "http://example.com");
    }
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

  abstract DropwizardAppExtension<CompatibilityTestApp.Config> getDW();

  abstract MongoDbClassExtension getMongo();
}
