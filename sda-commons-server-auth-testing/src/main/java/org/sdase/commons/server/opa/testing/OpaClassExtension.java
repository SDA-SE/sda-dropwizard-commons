package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockExtension;

@SuppressWarnings("WeakerAccess")
public class OpaClassExtension extends AbstractOpa implements BeforeAllCallback, AfterAllCallback {

  private final WireMockExtension wireMockExtension =
      new WireMockExtension(wireMockConfig().dynamicPort());

  private String opaClientTimeoutBackup = null;

  /**
   * @return the base url of the mock server
   */
  public String getUrl() {
    return wireMockExtension.baseUrl();
  }

  /** Resets all stubs and requests that were made to the mock server. */
  public void reset() {
    wireMockExtension.resetAll();
  }

  @Override
  public void beforeAll(final ExtensionContext context) {
    opaClientTimeoutBackup = System.getProperty(OPA_CLIENT_TIMEOUT);
    System.setProperty(OPA_CLIENT_TIMEOUT, OPA_CLIENT_TIMEOUT_DEFAULT);
    wireMockExtension.start();
    // by default all the requests will be denied. this can be overridden for each test case
    mock(onAnyRequest().deny());
  }

  @Override
  public void afterAll(final ExtensionContext context) {
    if (opaClientTimeoutBackup == null) {
      System.clearProperty(OPA_CLIENT_TIMEOUT);
    } else {
      System.setProperty(OPA_CLIENT_TIMEOUT, opaClientTimeoutBackup);
    }
    wireMockExtension.stop();
  }

  @Override
  void verify(int count, StubBuilder builder) {
    RequestPatternBuilder requestPattern = buildRequestPattern(builder);
    wireMockExtension.verify(count, requestPattern);
  }

  public void mock(BuildBuilder builder) {
    builder.build(wireMockExtension);
  }
}
