package org.sdase.commons.server.kafka.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminConfig {

  private int adminClientRequestTimeoutMs = 5000;

  private List<String> adminEndpoint = new ArrayList<>();

  private Security adminSecurity = new Security();

  private Map<String, String> config = new HashMap<>();

  public int getAdminClientRequestTimeoutMs() {
    return adminClientRequestTimeoutMs;
  }

  public void setAdminClientRequestTimeoutMs(int adminClientRequestTimeoutMs) {
    this.adminClientRequestTimeoutMs = adminClientRequestTimeoutMs;
  }

  public List<String> getAdminEndpoint() {
    return adminEndpoint;
  }

  public void setAdminEndpoint(List<String> adminEndpoint) {
    this.adminEndpoint = adminEndpoint;
  }

  public Security getAdminSecurity() {
    return adminSecurity;
  }

  public void setAdminSecurity(Security adminSecurity) {
    this.adminSecurity = adminSecurity;
  }

  public Map<String, String> getConfig() {
    return config;
  }

  public void setConfig(Map<String, String> config) {
    this.config = config;
  }
}
