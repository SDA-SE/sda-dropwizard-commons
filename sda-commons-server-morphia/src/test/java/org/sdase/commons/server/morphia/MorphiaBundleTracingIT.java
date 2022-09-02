package org.sdase.commons.server.morphia;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;

/** Tests if entities can be added by exact definition. */
public class MorphiaBundleTracingIT {

  private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

  private static final DropwizardAppRule<Config> DW =
      new DropwizardAppRule<>(
          MorphiaTestApp.class,
          null,
          randomPorts(),
          config("mongo.hosts", MONGODB::getHosts),
          config("mongo.database", MONGODB::getDatabase));

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

  @RegisterExtension static final OpenTelemetryExtension OTEL = OpenTelemetryExtension.create();

  @Test
  public void shouldHaveInstrumentation() {
    Datastore datastore = getDatastore();
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
  public void shouldNotTracePersonalData() {
    Datastore datastore = getDatastore();
    Person person = new Person().setAge(18).setName("Max");
    datastore.save(person);

    List<SpanData> spans = OTEL.getSpans();
    assertThat(spans)
        .extracting(SpanData::getName)
        .contains("createIndexes default_db.people", "insert default_db.people");
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

  private Datastore getDatastore() {
    return DW.<MorphiaTestApp>getApplication().getMorphiaBundle().datastore();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .withTelemetryInstance(OTEL.getOpenTelemetry())
            .build();

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
      bootstrap.addBundle(morphiaBundle);
    }

    @Override
    public void run(Config configuration, Environment environment) {
      // nothing to run
    }

    MorphiaBundle<Config> getMorphiaBundle() {
      return morphiaBundle;
    }
  }
}
