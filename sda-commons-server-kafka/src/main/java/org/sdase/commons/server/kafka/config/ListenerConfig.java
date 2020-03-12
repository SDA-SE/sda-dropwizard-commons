package org.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.consumer.strategies.legacy.LegacyMLS;
import org.sdase.commons.server.kafka.consumer.strategies.legacy.LegacyMLS.CommitType;

public class ListenerConfig {

  private int instances = 1;
  /**
   * @deprecated deprecated since configuration is not needed any longer if {@link
   *     MessageListenerStrategy} is used
   */
  @Deprecated private CommitType commitType = LegacyMLS.CommitType.SYNC; // NOSONAR

  /**
   * @deprecated deprecated since configuration is not needed any longer if {@link
   *     MessageListenerStrategy} is used
   */
  @Deprecated private boolean useAutoCommitOnly = true;

  private long topicMissingRetryMs = 0;
  private long pollInterval = 100;

  private ListenerConfig() {
    // empty constructor for jackson
  }

  public static ListenerConfig getDefault() {
    return new ListenerConfig();
  }

  public static ListenerConfigBuilder builder() {
    return new ListenerConfigBuilder();
  }

  public int getInstances() {
    return instances;
  }

  public void setInstances(int instances) {
    this.instances = instances;
  }

  /**
   * @deprecated deprecated since configuration is not needed any longer if {@link
   *     MessageListenerStrategy} is used
   * @return the commit type
   */
  @Deprecated
  public CommitType getCommitType() {
    return commitType;
  }

  /**
   * @deprecated deprecated since configuration is not needed any longer if {@link
   *     MessageListenerStrategy} is used
   * @param commitType the commit type
   */
  @Deprecated
  public void setCommitType(CommitType commitType) {
    this.commitType = commitType;
  }

  /**
   * @deprecated deprecated since configuration is not needed any longer if {@link
   *     MessageListenerStrategy} is used
   * @return true if use autocommit only
   */
  @Deprecated
  public boolean isUseAutoCommitOnly() {
    return useAutoCommitOnly;
  }

  /**
   * @deprecated deprecated since configuration is not needed any longer if {@link
   *     MessageListenerStrategy} is used
   * @param useAutoCommitOnly the use autocommit only value
   */
  @Deprecated
  public void setUseAutoCommitOnly(boolean useAutoCommitOnly) {
    this.useAutoCommitOnly = useAutoCommitOnly;
  }

  public long getTopicMissingRetryMs() {
    return topicMissingRetryMs;
  }

  public void setTopicMissingRetryMs(long topicMissingRetryMs) {
    this.topicMissingRetryMs = topicMissingRetryMs;
  }

  public long getPollInterval() {
    return pollInterval;
  }

  public void setPollInterval(long pollInterval) {
    this.pollInterval = pollInterval;
  }

  public static class ListenerConfigBuilder {

    private CommitType commitType = LegacyMLS.CommitType.SYNC; // NOSONAR
    private boolean useAutoCommitOnly = true;
    private long topicMissingRetryMs = 0;
    private long pollInterval = 100;

    /**
     * @deprecated deprecated since configuration is not needed any longer if {@link
     *     MessageListenerStrategy} is used
     * @param commitType the commit type
     * @return the same builder instance
     */
    @Deprecated
    public ListenerConfigBuilder withCommitType(@NotNull CommitType commitType) {
      this.commitType = commitType;
      return this;
    }

    /**
     * @deprecated deprecated since configuration is not needed any longer if {@link
     *     MessageListenerStrategy} is used
     * @param useAutoCommitOnly the useAutocommitOnly value
     * @return the same builder instance
     */
    @Deprecated
    public ListenerConfigBuilder useAutoCommitOnly(@NotNull boolean useAutoCommitOnly) {
      this.useAutoCommitOnly = useAutoCommitOnly;
      return this;
    }

    public ListenerConfigBuilder withTopicMissingRetryMs(@NotNull long ms) {
      this.topicMissingRetryMs = ms;
      return this;
    }

    public ListenerConfigBuilder withPollInterval(@NotNull long ms) {
      this.pollInterval = ms;
      return this;
    }

    public ListenerConfig build(@NotNull int numberInstances) {
      ListenerConfig build = new ListenerConfig();
      build.useAutoCommitOnly = useAutoCommitOnly; // NOSONAR
      build.commitType = commitType; // NOSONAR
      build.topicMissingRetryMs = topicMissingRetryMs;
      build.pollInterval = pollInterval;
      build.instances = numberInstances;
      return build;
    }
  }
}
