package org.sdase.commons.server.kms.aws.config;

import javax.validation.constraints.AssertTrue;

public class KmsAwsCachingConfiguration {

  /** Enable caching flag */
  private boolean enabled;
  /** Maximum capacity of the data key cache. */
  private int maxCacheSize;

  /** Maximum age in seconds a cached data key is used. */
  private int keyMaxLifetimeInSeconds;

  /** Maximum number of Kafka messages being encrypted with one data key from the cache. */
  private int maxMessagesPerKey;

  public int getMaxCacheSize() {
    return maxCacheSize;
  }

  public KmsAwsCachingConfiguration setMaxCacheSize(int maxCacheSize) {
    this.maxCacheSize = maxCacheSize;
    return this;
  }

  public int getKeyMaxLifetimeInSeconds() {
    return keyMaxLifetimeInSeconds;
  }

  public KmsAwsCachingConfiguration setKeyMaxLifetimeInSeconds(int keyMaxLifetimeInSeconds) {
    this.keyMaxLifetimeInSeconds = keyMaxLifetimeInSeconds;
    return this;
  }

  public int getMaxMessagesPerKey() {
    return maxMessagesPerKey;
  }

  public KmsAwsCachingConfiguration setMaxMessagesPerKey(int maxMessagesPerKey) {
    this.maxMessagesPerKey = maxMessagesPerKey;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @AssertTrue
  public boolean isValid() {
    if (enabled) {
      return maxCacheSize > 0 && keyMaxLifetimeInSeconds > 0 && maxMessagesPerKey > 0;
    }

    return true;
  }
}
