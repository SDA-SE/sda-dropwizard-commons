package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.testing.EnvironmentRule;

public class MongoDbRuleTest {
  @Test
  public void shouldStartLocalMongoDb() {
    new EnvironmentRule()
        .unsetEnv(MongoDbRule.OVERRIDE_MONGODB_CONNECTION_STRING_ENV_NAME)
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                MongoDbRule actualRule = MongoDbRule.builder().build();
                assertThat(actualRule).isExactlyInstanceOf(StartLocalMongoDbRule.class);
              }
            },
            Description.EMPTY);
  }
}
