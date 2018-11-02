package com.sdase.commons.server.kafka.exception;

import java.util.Set;
import java.util.stream.Collectors;

public class TopicMissingException extends RuntimeException {

   private final Set<String> missingTopics;

   public TopicMissingException(Set<String> missingTopics) {
      super(String.format("The following topics are missing %s", missingTopics.stream().collect(Collectors.joining(", "))));
      this.missingTopics = missingTopics;
   }

   public Set<String> getMissingTopics() {
      return missingTopics;
   }
}
