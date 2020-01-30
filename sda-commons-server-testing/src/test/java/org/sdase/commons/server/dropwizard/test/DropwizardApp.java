package org.sdase.commons.server.dropwizard.test;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;

public class DropwizardApp extends Application<DropwizardConfig> {

  @Override
  public void initialize(Bootstrap<DropwizardConfig> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
  }

  @Override
  public void run(DropwizardConfig configuration, Environment environment) {}
}
