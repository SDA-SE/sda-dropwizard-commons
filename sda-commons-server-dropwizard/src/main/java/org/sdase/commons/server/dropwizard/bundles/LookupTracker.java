package org.sdase.commons.server.dropwizard.bundles;

import java.util.Set;

/** Tracks keys that were looked up in the environment. */
public interface LookupTracker {

  /**
   * @return all keys that have been looked up, whether they led to a result or not.
   */
  Set<String> lookedUpKeys();
}
