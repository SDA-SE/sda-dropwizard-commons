package org.sdase.commons.server.auth.config;

import java.util.ArrayList;
import java.util.List;

/** Configuration for authentication using JWT. */
public class AuthConfig {

  /** Keys that are allowed to sign tokens. */
  private List<KeyLocation> keys = new ArrayList<>();

  /**
   * The default window in seconds in which the Not Before, Issued At and Expires At Claims will
   * still be valid.
   */
  private long leeway = 0;

  /**
   * Used to disable authentication for local development and unit testing. Should NEVER be set in
   * production.
   */
  private boolean disableAuth;

  public List<KeyLocation> getKeys() {
    return keys;
  }

  public AuthConfig setKeys(List<KeyLocation> keys) {
    this.keys = keys;
    return this;
  }

  public long getLeeway() {
    return leeway;
  }

  public AuthConfig setLeeway(long leeway) {
    this.leeway = leeway;
    return this;
  }

  public boolean isDisableAuth() {
    return disableAuth;
  }

  public AuthConfig setDisableAuth(boolean disableAuth) {
    this.disableAuth = disableAuth;
    return this;
  }
}
