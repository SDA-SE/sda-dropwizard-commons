package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Date;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

class SpringDataMongoBundleCustomConverterOverwriteIT {

  @RegisterExtension
  @Order(0)
  static final MongoDbClassExtension mongo = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<MyConfiguration> DW =
      new DropwizardAppExtension<>(
          DefaultConvertersOverwrite.class,
          null,
          config("springDataMongo.connectionString", mongo::getConnectionString));

  @BeforeEach
  void setup() {
    mongo.clearCollections();
  }

  @Test
  void customConvertersShouldOverwriteSpringConvertersForWritingConverters() {
    MongoOperations mongoOperations = getMongoOperations();

    // given
    var car =
        new Car()
            .setName("SomeCar")
            .setTopSpeed(new BigDecimal(300))
            .setLastTripDuration(Period.ofDays(1));

    // when
    mongoOperations.save(car);

    // and
    var blobFromDb = mongoOperations.findAll(String.class, "cars").stream().findFirst();

    // then
    assertThat(blobFromDb).isPresent();
    assertThat(blobFromDb.get()).contains("fast");
    assertThat(blobFromDb.get()).contains("period");
  }

  @Test
  void customConvertersShouldOverwriteSpringConvertersForReadingConverters() {
    MongoOperations mongoOperations = getMongoOperations();

    // given
    var car = new Car().setName("SomeCar").setLastUsed(LocalDateTime.now());

    // when
    mongoOperations.save(car);

    // and
    var entitybFromDb = mongoOperations.findAll(Car.class, "cars").stream().findFirst();

    // then
    assertThat(entitybFromDb).isPresent();
    assertThat(entitybFromDb.get().getLastUsed())
        .isEqualToIgnoringSeconds(LocalDateTime.of(2000, 1, 1, 1, 1));
  }

  private MongoOperations getMongoOperations() {
    return DW.<DefaultConvertersOverwrite>getApplication().getMongoOperations();
  }

  public static class DefaultConvertersOverwrite extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(Car.class)
            .addCustomConverters(
                new CustomBigDecimalToStringConverter(),
                new CustomDateToLocalDateTimeConverter(),
                new CustomPeriodToStringConverter())
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

  @WritingConverter
  public static class CustomBigDecimalToStringConverter implements Converter<BigDecimal, String> {

    @Override
    public String convert(BigDecimal source) {
      return "fast";
    }
  }

  @ReadingConverter
  public static class CustomDateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {

    @Override
    public LocalDateTime convert(Date source) {
      return LocalDateTime.of(2000, 1, 1, 1, 1);
    }
  }

  @WritingConverter
  public static class CustomPeriodToStringConverter implements Converter<Period, String> {

    @Override
    public String convert(Period period) {
      return "period";
    }
  }

  @Document("cars")
  public class Car {
    @MongoId private ObjectId id;

    @Indexed @NotNull private String name;

    private LocalDateTime lastUsed;

    private Period lastTripDuration;

    @Min(1)
    @Indexed
    private BigDecimal topSpeed;

    public ObjectId getId() {
      return id;
    }

    public Car setId(ObjectId id) {
      this.id = id;
      return this;
    }

    public String getName() {
      return name;
    }

    public Car setName(String name) {
      this.name = name;
      return this;
    }

    public BigDecimal getTopSpeed() {
      return topSpeed;
    }

    public Car setTopSpeed(BigDecimal topSpeed) {
      this.topSpeed = topSpeed;
      return this;
    }

    public LocalDateTime getLastUsed() {
      return lastUsed;
    }

    public Car setLastUsed(LocalDateTime lastUsed) {
      this.lastUsed = lastUsed;
      return this;
    }

    public Period getLastTripDuration() {
      return lastTripDuration;
    }

    public Car setLastTripDuration(Period lastTripDuration) {
      this.lastTripDuration = lastTripDuration;
      return this;
    }
  }
}
