package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@SuppressWarnings("WeakerAccess")
/** @deprecated migrate to Junit 5 and use {@link OpaExtension} */
@Deprecated
public class OpaRule extends AbstractOpa implements TestRule {

  private static final ObjectMapper OM = new ObjectMapper();

  private final WireMockClassRule wire = new WireMockClassRule(wireMockConfig().dynamicPort());

  /**
   * Create a builder to match a request.
   *
   * @return the builder
   */
  public static RequestMethodBuilder onRequest() {
    return new StubBuilder();
  }

  public static AllowBuilder onAnyRequest() {
    return new StubBuilder().onAnyRequest();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return RuleChain.outerRule(wire).apply(base, description);
  }

  public String getUrl() {
    return wire.baseUrl();
  }

  public void reset() {
    wire.resetAll();
  }

  //  @Before
  //  protected void before() {
  //    wire.start();
  //  }
  //
  //  @After
  //  protected void after() {
  //    wire.stop();
  //  }

  /**
   * Verify if the policy was called for the method/path
   *
   * <p>Please prefer to use {@code verify(count,
   * onRequest().withHttpMethod(httpMethod).withPath(path))} instead.
   *
   * @param count the expected count of calls
   * @param httpMethod the HTTP method to check
   * @param path the path to check
   */
  public void verify(int count, String httpMethod, String path) {
    verify(count, onRequest().withHttpMethod(httpMethod).withPath(path));
  }

  public void verify(int count, AllowBuilder allowBuilder) {
    verify(count, (StubBuilder) allowBuilder);
  }

  private void verify(int count, StubBuilder builder) {
    RequestPatternBuilder requestPattern = buildRequestPattern(builder);
    wire.verify(count, requestPattern);
  }

  public void mock(BuildBuilder builder) {
    builder.build(wire);
  }
}
