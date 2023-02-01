package org.sdase.commons.server.dropwizard.bundles;

import org.apache.commons.text.lookup.StringLookup;

/**
 * Lookup for Java's system properties and environment variables. System properties have higher
 * priority.
 */
public class SystemPropertyAndEnvironmentLookup implements StringLookup {

  @Override
  public String lookup(String key) {
    String result = System.getProperty(key);
    if (result != null) {
      return result;
    }
    return System.getenv(key);
  }
}
