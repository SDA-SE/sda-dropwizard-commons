package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MongoDbVersionTest {

  @Test
  public void shouldTakeSpecificMongoDbVersion() {
    IFeatureAwareVersion specificMongoDbVersion = Version.V3_2_20;
    MongoDbRule mongoDbRule = MongoDbRule.builder().withVersion(specificMongoDbVersion).build();
    assertThat(mongoDbRule).extracting("version").isEqualTo(specificMongoDbVersion);
  }

  @Test
  public void shouldStartSpecificMongoDbVersion() throws Throwable {
    AtomicReference<String> actualVersion = new AtomicReference<>();
    final IFeatureAwareVersion specificMongoDbVersion = Version.V3_2_20;
    MongoDbRule mongoDbRule = MongoDbRule.builder().withVersion(specificMongoDbVersion).build();
    mongoDbRule
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                actualVersion.set(mongoDbRule.getServerVersion());
              }
            },
            Description.EMPTY)
        .evaluate();
    assertThat(actualVersion.get()).isEqualTo("3.2.20");
  }

  @Test
  public void shouldDetermineMongoDbVersionIfVersionIsNull() {
    final MongoDbRule mongoDbRule = MongoDbRule.builder().withVersion(null).build();
    assertThat(mongoDbRule)
        .extracting("version")
        .isIn(MongoDbRule.Builder.DEFAULT_VERSION, MongoDbRule.Builder.WINDOWS_VERSION);
  }

  @Test
  public void shouldUseOsSpecificMongoDbVersion() {
    MongoDbRule mongoDbRule = MongoDbRule.builder().build();
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(mongoDbRule).extracting("version").isEqualTo(MongoDbRule.Builder.WINDOWS_VERSION);
    } else {
      assertThat(mongoDbRule).extracting("version").isEqualTo(MongoDbRule.Builder.DEFAULT_VERSION);
    }
  }
}
