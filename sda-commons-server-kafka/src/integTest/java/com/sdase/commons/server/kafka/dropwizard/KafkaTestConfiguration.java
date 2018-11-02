package com.sdase.commons.server.kafka.dropwizard;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.salesforce.kafka.test.KafkaBrokers;
import com.sdase.commons.server.kafka.KafkaConfiguration;
import com.sdase.commons.server.kafka.config.Broker;
import io.dropwizard.Configuration;

public class KafkaTestConfiguration extends Configuration {

   private static final String TEST_GROUP = "group";
   private static final int N_THREADS = 1;

   @JsonProperty
   private KafkaConfiguration kafka = new KafkaConfiguration();


   public KafkaTestConfiguration() {
   }

   public KafkaTestConfiguration withBrokers(KafkaBrokers kafkaBrokers) {
      kafkaBrokers.forEach(b -> {
         String [] split = b.getConnectString().split(":");
         Broker broker = new Broker();
         broker.setServer(split[0]);
         broker.setPort(Integer.parseInt(split[1]));
         kafka.getBrokers().add(broker);
      });
      return this;
   }


   public KafkaConfiguration getKafka() {
      return kafka;
   }

}
