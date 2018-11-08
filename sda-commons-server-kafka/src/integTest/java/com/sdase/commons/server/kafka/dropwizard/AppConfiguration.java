package com.sdase.commons.server.kafka.dropwizard;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sdase.commons.server.kafka.KafkaConfiguration;
import io.dropwizard.Configuration;

public class AppConfiguration extends Configuration {

   @Valid
   @NotNull
   private KafkaConfiguration kafka = new KafkaConfiguration();

   public KafkaConfiguration getKafka() {
      return kafka;
   }

}
