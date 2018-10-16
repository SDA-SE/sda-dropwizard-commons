package com.sdase.commons.server.jackson.test;

import io.dropwizard.Configuration;

public class JacksonConfigurationTestAppConfig extends Configuration {

   private boolean disableFieldFilter;

   public boolean isDisableFieldFilter() {
      return disableFieldFilter;
   }

   public JacksonConfigurationTestAppConfig setDisableFieldFilter(boolean disableFieldFilter) {
      this.disableFieldFilter = disableFieldFilter;
      return this;
   }
}
