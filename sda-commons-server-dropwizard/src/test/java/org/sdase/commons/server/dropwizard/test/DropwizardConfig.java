package org.sdase.commons.server.dropwizard.test;

import io.dropwizard.Configuration;

public class DropwizardConfig extends Configuration {

   private String envReplaced;

   private String envDefault;

   private String envWithDefaultReplaced;

   private String envMissing;

   public String getEnvReplaced() {
      return envReplaced;
   }

   public DropwizardConfig setEnvReplaced(String envReplaced) {
      this.envReplaced = envReplaced;
      return this;
   }

   public String getEnvDefault() {
      return envDefault;
   }

   public DropwizardConfig setEnvDefault(String envDefault) {
      this.envDefault = envDefault;
      return this;
   }

   public String getEnvWithDefaultReplaced() {
      return envWithDefaultReplaced;
   }

   public DropwizardConfig setEnvWithDefaultReplaced(String envWithDefaultReplaced) {
      this.envWithDefaultReplaced = envWithDefaultReplaced;
      return this;
   }

   public String getEnvMissing() {
      return envMissing;
   }

   public DropwizardConfig setEnvMissing(String envMissing) {
      this.envMissing = envMissing;
      return this;
   }
}
