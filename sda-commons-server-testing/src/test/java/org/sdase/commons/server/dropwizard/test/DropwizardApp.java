package org.sdase.commons.server.dropwizard.test;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;

public class DropwizardApp extends Application<DropwizardConfig> {

  @Override
  public void initialize(Bootstrap<DropwizardConfig> bootstrap) {
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
  }

  @Override
  public void run(DropwizardConfig configuration, Environment environment) {}
}
