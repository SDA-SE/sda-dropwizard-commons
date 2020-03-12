package org.sdase.commons.server.opa.config;

import javax.validation.constraints.NotNull;

/** Configuration for requesting OPA PDP. */
@SuppressWarnings("UnusedReturnValue")
public class OpaConfig {

  /** flag if OPA is disabled (for testing) */
  private boolean disableOpa;

  /** base url where to find the OPA */
  private String baseUrl = "http://localhost:8181";

  /**
   * dot-separated package name as defined in the policy
   *
   * <p>The package name is reformatted as part of the URL. Test {@code my.policy} becomes {@code
   * my/policy}
   */
  @NotNull private String policyPackage = "";

  /** readTimeout for opa requests in milliseconds */
  private int readTimeout = 500;

  public boolean isDisableOpa() {
    return disableOpa;
  }

  public OpaConfig setDisableOpa(boolean disableOpa) {
    this.disableOpa = disableOpa;
    return this;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public OpaConfig setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  public String getPolicyPackage() {
    return policyPackage;
  }

  public OpaConfig setPolicyPackage(String policyPackage) {
    this.policyPackage = policyPackage;
    return this;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  public OpaConfig setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  public String getPolicyPackagePath() {
    return policyPackage.replaceAll("\\.", "/").trim();
  }
}
