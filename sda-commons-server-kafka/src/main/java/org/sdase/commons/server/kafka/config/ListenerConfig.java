package org.sdase.commons.server.kafka.config;

import org.sdase.commons.server.kafka.consumer.MessageListener;

import javax.validation.constraints.NotNull;
import org.sdase.commons.server.kafka.consumer.MessageListener.CommitType;

public class ListenerConfig {

   public static ListenerConfig getDefault() {
      return new ListenerConfig();
   }

   private int instances = 1;
   private MessageListener.CommitType commitType = MessageListener.CommitType.SYNC;
   private boolean useAutoCommitOnly = true;
   private long topicMissingRetryMs = 0;
   private long pollInterval = 100;

   private ListenerConfig() {
      // empty constructor for jackson
   }

   public void setInstances(int instances) {
      this.instances = instances;
   }

   public void setCommitType(CommitType commitType) {
      this.commitType = commitType;
   }

   public void setUseAutoCommitOnly(boolean useAutoCommitOnly) {
      this.useAutoCommitOnly = useAutoCommitOnly;
   }

   public void setTopicMissingRetryMs(long topicMissingRetryMs) {
      this.topicMissingRetryMs = topicMissingRetryMs;
   }

   public void setPollInterval(long pollInterval) {
      this.pollInterval = pollInterval;
   }

   public int getInstances() {
      return instances;
   }

   public MessageListener.CommitType getCommitType() {
      return commitType;
   }

   public boolean isUseAutoCommitOnly() {
      return useAutoCommitOnly;
   }

   public long getTopicMissingRetryMs() {
      return topicMissingRetryMs;
   }

   public long getPollInterval() {
      return pollInterval;
   }

   public static ListenerConfigBuilder builder() {
      return new ListenerConfigBuilder();
   }

   public static class ListenerConfigBuilder {

      private MessageListener.CommitType commitType = MessageListener.CommitType.SYNC;
      private boolean useAutoCommitOnly = true;
      private long topicMissingRetryMs = 0;
      private long pollInterval = 100;

      public ListenerConfigBuilder withCommitType(@NotNull MessageListener.CommitType commitType) {
         this.commitType = commitType;
         return this;
      }

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
         build.useAutoCommitOnly = useAutoCommitOnly;
         build.commitType = commitType;
         build.topicMissingRetryMs = topicMissingRetryMs;
         build.pollInterval = pollInterval;
         build.instances = numberInstances;
         return build;
      }
   }
}
