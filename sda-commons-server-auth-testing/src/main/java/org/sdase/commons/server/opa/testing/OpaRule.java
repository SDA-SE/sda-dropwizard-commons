package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.testing.SystemPropertyRule;

/**
 * @deprecated Please update to JUnit 5 and use {@link OpaClassExtension}
 */
@Deprecated
@SuppressWarnings("WeakerAccess")
public class OpaRule extends AbstractOpa implements TestRule {

  private final WireMockClassRule wireMockClassRule =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private final SystemPropertyRule systemPropertyRule =
      new SystemPropertyRule().setProperty(OPA_CLIENT_TIMEOUT, OPA_CLIENT_TIMEOUT_DEFAULT);

  @Override
  public Statement apply(Statement base, Description description) {
    return RuleChain.outerRule(wireMockClassRule)
        .around(systemPropertyRule)
        .apply(base, description);
  }

  public String getUrl() {
    return wireMockClassRule.baseUrl();
  }

  public void reset() {
    wireMockClassRule.resetAll();
  }

  @Override
  void verify(int count, StubBuilder builder) {
    RequestPatternBuilder requestPattern = buildRequestPattern(builder);
    wireMockClassRule.verify(count, requestPattern);
  }

  public void mock(BuildBuilder builder) {
    builder.build(wireMockClassRule);
  }
}
