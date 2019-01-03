package org.sdase.commons.server.kafka;

import io.dropwizard.Configuration;

public class KafkaExampleConfiguration extends Configuration {

   private KafkaConfiguration kafka = new KafkaConfiguration();

   KafkaConfiguration getKafka() {
      return kafka;
   }


}
