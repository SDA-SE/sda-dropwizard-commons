package com.sdase.commons.server.dropwizard.test;

import com.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class DropwizardApp extends Application<DropwizardConfig> {

   @Override
   public void initialize(Bootstrap<DropwizardConfig> bootstrap) {
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
   }

   @Override
   public void run(DropwizardConfig configuration, Environment environment) {

   }
}
