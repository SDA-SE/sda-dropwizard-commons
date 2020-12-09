package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.testing.EnvironmentRule;

public class MongoDbRuleTest {

  @Test
  public void shouldUseExistingMongoDb() throws Throwable {
    AtomicReference<MongoDbRule> actualRule = new AtomicReference<>();
    new EnvironmentRule()
        .setEnv(MongoDbRule.OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME, "mongodb://localhost")
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                actualRule.set(MongoDbRule.builder().build());
              }
            },
            Description.EMPTY)
        .evaluate();
    assertThat(actualRule.get()).isExactlyInstanceOf(UseExistingMongoDbRule.class);
  }

  @Test
  public void shouldStartLocalMongoDb() throws Throwable {
    AtomicReference<MongoDbRule> actualRule = new AtomicReference<>();
    new EnvironmentRule()
        .unsetEnv(MongoDbRule.OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME)
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                actualRule.set(MongoDbRule.builder().build());
              }
            },
            Description.EMPTY)
        .evaluate();
    assertThat(actualRule.get()).isExactlyInstanceOf(StartLocalMongoDbRule.class);
  }
}
