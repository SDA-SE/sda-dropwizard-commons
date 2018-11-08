package com.sdase.commons.server.kafka.topicana;

import com.github.ftrossbach.club_topicana.core.ExpectedTopicConfiguration;



public class TopicConfigurationBuilder {

   private TopicConfigurationBuilder() {
      // private constructor to hide the explicit one
   }
   
   public static ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder builder(String topic) {
      return new ExpectedTopicConfiguration.ExpectedTopicConfigurationBuilder(topic);
   }

}
