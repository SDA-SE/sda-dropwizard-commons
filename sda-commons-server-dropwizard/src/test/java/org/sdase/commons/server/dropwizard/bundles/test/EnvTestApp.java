package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.dropwizard.bundles.EnvironmentVariableConfigurationBundle;

public class EnvTestApp extends Application<EnvTestApp.EnvConfiguration> {

  @Override
  public void initialize(Bootstrap<EnvConfiguration> bootstrap) {
    bootstrap.addBundle(EnvironmentVariableConfigurationBundle.builder().build());
  }

  @Override
  public void run(EnvConfiguration configuration, Environment environment) {
    // nothing
  }

  public static class EnvConfiguration extends Configuration {
    private final Map<String, Object> config = new HashMap<>();
    private final List<String> arrayConfig = new ArrayList<>();

    public Map<String, Object> getConfig() {
      return config;
    }

    public List<String> getArrayConfig() {
      return arrayConfig;
    }
  }
}
