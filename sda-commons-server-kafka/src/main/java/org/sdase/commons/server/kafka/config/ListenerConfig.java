package org.sdase.commons.server.kafka.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sdase.commons.server.kafka.consumer.MessageListener;

import javax.validation.constraints.NotNull;

public class ListenerConfig {

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

   public static ListenerConfig getDefault() {
      return new ListenerConfig();
   }

   public static ListenerConfigBuilder builder() {
      return new ListenerConfigBuilder();
   }


   @JsonProperty(value = "instances", defaultValue = "1")
   private int instances = 1;

   @JsonProperty(value = "commitType", defaultValue = "SYNC")
   private MessageListener.CommitType commitType = MessageListener.CommitType.SYNC;

   @JsonProperty(value = "useAutoCommitOnly", defaultValue = "true")
   private boolean useAutoCommitOnly = true;

   @JsonProperty(value = "topicMissingRetryMs", defaultValue = "0")
   private long topicMissingRetryMs = 0;

   @JsonProperty(value = "pollInterval", defaultValue = "100")
   private long pollInterval = 100;


   private ListenerConfig() {
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
}
