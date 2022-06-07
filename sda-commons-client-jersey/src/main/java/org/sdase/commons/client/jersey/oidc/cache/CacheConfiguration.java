package org.sdase.commons.client.jersey.oidc.cache;

public class CacheConfiguration {

  private boolean disabled = false;
  private boolean autoRefresh = false;

  public boolean isDisabled() {
    return disabled;
  }

  public CacheConfiguration setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  public boolean isAutoRefresh() {
    return autoRefresh;
  }

  public CacheConfiguration setAutoRefresh(boolean autoRefresh) {
    this.autoRefresh = autoRefresh;
    return this;
  }
}
