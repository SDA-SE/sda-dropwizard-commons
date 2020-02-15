package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.sdase.commons.server.mongo.testing.MongoDbRule.Builder.DEFAULT_DATABASE;

import com.mongodb.BasicDBObject;
import dev.morphia.Datastore;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.morphia.test.Config;
import org.sdase.commons.server.morphia.test.model.Person;
import org.sdase.commons.server.testing.DropwizardRuleHelper;
import org.sdase.commons.server.testing.LazyRule;

/** Tests if entities can be added by exact definition. */
public class MorphiaBundleTracingIT {

  private static final MongoDbRule MONGODB = MongoDbRule.builder().build();

  private static final LazyRule<DropwizardAppRule<Config>> DW =
      new LazyRule<>(
          () ->
              DropwizardRuleHelper.dropwizardTestAppFrom(MorphiaTestApp.class)
                  .withConfigFrom(Config::new)
                  .withRandomPorts()
                  .withConfigurationModifier(
                      c -> c.getMongo().setHosts(MONGODB.getHost()).setDatabase("testPeople"))
                  .build());

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(MONGODB).around(DW);

  @Test
  public void shouldHaveInstrumentation() {
    Datastore datastore = getMorphiaBundle().datastore();
    Person person = new Person().setAge(18).setName("Max");
    datastore.save(person);

    MockTracer tracer = getMockTracer();
    await()
        .untilAsserted(
            () -> {
              assertThat(tracer.finishedSpans())
                  .extracting(MockSpan::operationName)
                  .contains("createIndexes", "insert");
              assertThat(
                      tracer.finishedSpans().stream()
                          .map(s -> s.tags().keySet())
                          .flatMap(Set::stream)
                          .noneMatch(Tags.DB_STATEMENT::equals))
                  .isTrue();
            });
  }

  @Test
  public void shouldExcludePingCommand() {
    BasicDBObject ping = new BasicDBObject("ping", "1");
    getMorphiaBundle().mongoClient().getDatabase(DEFAULT_DATABASE).runCommand(ping);

    MockTracer tracer = getMockTracer();
    await()
        .pollDelay(1, TimeUnit.SECONDS)
        .untilAsserted(
            () ->
                assertThat(tracer.finishedSpans())
                    .extracting(MockSpan::operationName)
                    .doesNotContain("ping"));
  }

  private MorphiaBundle<Config> getMorphiaBundle() {
    return ((MorphiaTestApp) DW.getRule().getApplication()).getMorphiaBundle();
  }

  private MockTracer getMockTracer() {
    return ((MorphiaTestApp) DW.getRule().getApplication()).getMockTracer();
  }

  public static class MorphiaTestApp extends Application<Config> {

    private MockTracer mockTracer = new MockTracer();

    private MorphiaBundle<Config> morphiaBundle =
        MorphiaBundle.builder()
            .withConfigurationProvider(Config::getMongo)
            .withEntity(Person.class)
            .withTracer(mockTracer)
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

    MockTracer getMockTracer() {
      return mockTracer;
    }
  }
}
