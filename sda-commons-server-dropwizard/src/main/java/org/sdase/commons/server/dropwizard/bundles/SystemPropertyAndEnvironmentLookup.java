package org.sdase.commons.server.dropwizard.bundles;

import io.dropwizard.configuration.EnvironmentVariableLookup;

/**
 * Lookup for Java's system properties and environment variables. System properties have higher
 * priority.
 */
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
