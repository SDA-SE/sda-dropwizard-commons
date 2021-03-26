package org.sdase.commons.client.jersey.oidc.cache;

public class CacheConfiguration {

  private boolean disabled = false;

  public boolean isDisabled() {
    return disabled;
  }

  public CacheConfiguration setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }
}
