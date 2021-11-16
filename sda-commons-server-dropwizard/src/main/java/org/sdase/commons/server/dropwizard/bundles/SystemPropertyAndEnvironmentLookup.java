package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.configuration.EnvironmentVariableLookup;

public class SystemPropertyAndEnvironmentLookup extends EnvironmentVariableLookup {

  @Override
  public String lookup(String key) {
    String result = System.getProperty(key);
    if (result != null) {
      return result;
    }
    return super.lookup(key);
  }
}
