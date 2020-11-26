package org.sdase.commons.server.kms.aws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class KmsAwsCachingConfigurationTest {
  KmsAwsCachingConfiguration classUnderTest = new KmsAwsCachingConfiguration();

  @Test
  public void cacheDisabledTest() {
    classUnderTest.setEnabled(false);
    classUnderTest.setMaxCacheSize(0);
    classUnderTest.setKeyMaxLifetimeInSeconds(0);
    classUnderTest.setMaxMessagesPerKey(0);

    assertThat(classUnderTest.isValid()).isTrue();
  }

  @Test
  public void cacheEnabledAndValidTest() {
    classUnderTest.setEnabled(true);
    classUnderTest.setMaxCacheSize(1);
    classUnderTest.setKeyMaxLifetimeInSeconds(1);
    classUnderTest.setMaxMessagesPerKey(1);

    assertTrue(classUnderTest.isValid());
  }

  @Test
  public void cacheInvalidConfigKeyMaxLifetimeInSecondsIsZeroTest() {
    classUnderTest.setEnabled(true);
    classUnderTest.setMaxCacheSize(1);
    classUnderTest.setKeyMaxLifetimeInSeconds(0);
    classUnderTest.setMaxMessagesPerKey(1);

    assertThat(classUnderTest.isValid()).isFalse();
  }

  @Test
  public void cacheInvalidConfigMaxMessagesPerKeyIsZeroTest() {
    classUnderTest.setEnabled(true);
    classUnderTest.setMaxCacheSize(1);
    classUnderTest.setKeyMaxLifetimeInSeconds(1);
    classUnderTest.setMaxMessagesPerKey(0);

    assertThat(classUnderTest.isValid()).isFalse();
  }
}
