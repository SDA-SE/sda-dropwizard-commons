package org.sdase.commons.server.opa.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.NotNull;

/** Configuration for requesting OPA PDP. */
@SuppressWarnings("UnusedReturnValue")
public class OpaConfig {
  /** The client configuration of the HTTP client that is used to call the Open Policy Agent. */
  private OpaClientConfiguration opaClient = new OpaClientConfiguration();

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

  /**
   * readTimeout for opa requests in milliseconds
   *
   * @deprecated The client that is used to call the OPA is now configurable. Prefer to configure
   *     {@code opaClient.timeout} instead:
   *     <pre>{@code
   * opa:
   *   opaClient:
   *     timeout: 500ms
   * }</pre>
   */
  @Deprecated private Integer readTimeout;

  public OpaClientConfiguration getOpaClient() {
    return opaClient;
  }

  public OpaConfig setOpaClient(OpaClientConfiguration opaClient) {
    this.opaClient = opaClient;
    return this;
  }

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

  /**
   * @deprecated The client that is used to call the OPA is now configurable. Prefer to configure
   *     {@code opaClient.timeout} instead:
   *     <pre>{@code
   * opa:
   *   opaClient:
   *     timeout: 500ms
   * }</pre>
   */
  @Deprecated
  public Integer getReadTimeout() {
    return readTimeout;
  }

  /**
   * @param readTimeout the read timeout
   * @deprecated The client that is used to call the OPA is now configurable. Prefer to configure
   *     {@code opaClient.timeout} instead:
   *     <pre>{@code
   * opa:
   *   opaClient:
   *     timeout: 500ms
   * }</pre>
   */
  @Deprecated
  public OpaConfig setReadTimeout(Integer readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  @JsonIgnore
  public String getPolicyPackagePath() {
    return policyPackage.replaceAll("\\.", "/").trim();
  }
}
