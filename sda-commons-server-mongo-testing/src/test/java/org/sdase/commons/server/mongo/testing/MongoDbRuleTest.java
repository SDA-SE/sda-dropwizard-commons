package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.testing.SystemPropertyRule;

class MongoDbRuleTest {

  @Test
  void shouldUseExistingMongoDb() throws Throwable {
    AtomicReference<MongoDbRule> actualRule = new AtomicReference<>();
    new SystemPropertyRule()
        .setProperty(
            MongoDbRule.OVERRIDE_MONGODB_CONNECTION_STRING_SYSTEM_PROPERTY_NAME,
            "mongodb://localhost")
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
  void shouldStartLocalMongoDb() throws Throwable {
    AtomicReference<MongoDbRule> actualRule = new AtomicReference<>();
    new SystemPropertyRule()
        .unsetProperty(MongoDbRule.OVERRIDE_MONGODB_CONNECTION_STRING_SYSTEM_PROPERTY_NAME)
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
