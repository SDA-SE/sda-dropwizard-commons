package org.sdase.commons.server.spring.data.mongo;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;
import org.sdase.commons.server.spring.data.mongo.example.MyConfiguration;
import org.sdase.commons.server.spring.data.mongo.example.model.Person;
import org.springframework.data.mongodb.core.MongoOperations;

/**
 * Using ordered test execution here because the indexes will only be created once. Seems to be a
 * Spring issue. Since you usually don't drop databases in your application, this is sufficient for
 * us.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MongoBundleTracingIT {

  @RegisterExtension
  @Order(0)
  private static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @RegisterExtension
  @Order(1)
  private static final MongoDbClassExtension MONGODB = MongoDbClassExtension.builder().build();

  @RegisterExtension
  @Order(2)
  private static final DropwizardAppExtension<MyConfiguration> DW =
      new DropwizardAppExtension<>(
          TracingTestApp.class,
          null,
          randomPorts(),
          config("springDataMongo.connectionString", MONGODB::getConnectionString));

  @BeforeEach
  void beforeEach() {
    MONGODB.clearDatabase();
  }

  @Test
  @Order(0)
  void shouldHaveInstrumentationForCreateIndexesAndInsert() {
    var datastore = getDatastore();
    Person person = new Person().setAge(18).setName("Max");
    datastore.save(person);

    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans)
        .extracting(SpanData::getName)
        .contains("createIndexes default_db.people", "insert default_db.people");
    assertThat(
            spans.stream()
                .map(SpanData::getAttributes)
                .map(attributes -> attributes.get(SemanticAttributes.DB_STATEMENT))
                .collect(Collectors.toList()))
        .isNotEmpty();
  }

  @Test
  @Order(1)
  void shouldNotTracePersonalData() {
    var datastore = getDatastore();
    Person person = new Person().setAge(18).setName("Max");
    datastore.save(person);

    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans).extracting(SpanData::getName).contains("insert default_db.people");
    List<String> dbStatements =
        spans.stream()
            .map(SpanData::getAttributes)
            .map(attributes -> attributes.get(SemanticAttributes.DB_STATEMENT))
            .collect(Collectors.toList());
    assertThat(dbStatements)
        .isNotEmpty()
        .anyMatch(v -> v.contains("\"name\": \"?\""))
        .anyMatch(v -> v.contains("\"age\": \"?\""))
        .noneMatch(v -> v.contains("\"age\": 18"))
        .noneMatch(v -> v.contains("\"name\": \"Max\""));
  }

  private MongoOperations getDatastore() {
    return DW.<TracingTestApp>getApplication().getSpringDataMongoBundle().getMongoOperations();
  }

  public static class TracingTestApp extends Application<MyConfiguration> {

    private final SpringDataMongoBundle<MyConfiguration> springDataMongoBundle =
        SpringDataMongoBundle.builder()
            .withConfigurationProvider(MyConfiguration::getSpringDataMongo)
            .withEntities(Person.class)
            .withTelemetryInstance(OTEL.getOpenTelemetry())
            .build();

    @Override
    public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      bootstrap.addBundle(springDataMongoBundle);
    }

    @Override
    public void run(MyConfiguration configuration, Environment environment) {
      // nothing to run
    }

    SpringDataMongoBundle<MyConfiguration> getSpringDataMongoBundle() {
      return springDataMongoBundle;
    }
  }
}
