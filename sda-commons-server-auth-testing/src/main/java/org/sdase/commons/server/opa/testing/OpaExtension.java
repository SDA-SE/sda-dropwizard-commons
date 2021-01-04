package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockExtension;
import org.sdase.commons.server.opa.filter.model.OpaResponse;

@SuppressWarnings("WeakerAccess")
public class OpaExtension extends AbstractOpa implements BeforeAllCallback, AfterAllCallback {

  private static final ObjectMapper OM = new ObjectMapper();

  private final WireMockExtension wire = new WireMockExtension(wireMockConfig().dynamicPort());

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

  public String getUrl() {
    return wire.baseUrl();
  }

  public void reset() {
    wire.resetAll();
  }

  @Override
  public void beforeAll(final ExtensionContext context) {
    wire.start();
  }

  public void afterAll(final ExtensionContext context) {
    wire.stop();
  }

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
    verify(count, (AbstractStubBuilder) allowBuilder);
  }

  private void verify(int count, AbstractStubBuilder builder) {
    RequestPatternBuilder requestPattern = buildRequestPattern(builder);
    wire.verify(count, requestPattern);
  }

  public void mock(BuildBuilder builder) {
    builder.build(wire);
  }

  public static class StubBuilder extends AbstractStubBuilder {
    @Override
    public void build(Object wire) {
      WireMockExtension wireMockExtension = (WireMockExtension) wire;
      MappingBuilder mappingBuilder;
      if (isOnAnyRequest()) {
        mappingBuilder = matchAnyPostUrl();
      } else {
        mappingBuilder = matchInput(getHttpMethod(), getPaths());

        if (isMatchJWT()) {
          mappingBuilder.withRequestBody(
              matchingJsonPath("$.input.jwt", getJwt() != null ? equalTo(getJwt()) : absent()));
        }
      }

      if (isErrorResponse()) {
        wireMockExtension.stubFor(mappingBuilder.willReturn(aResponse().withStatus(500)));
        return;
      }

      if (isErrorResponse()) {
        wireMockExtension.stubFor(mappingBuilder.willReturn(null));
        return;
      }

      OpaResponse response;
      if (getAnswer() != null) {
        response = getAnswer();
      } else {
        ObjectNode objectNode = OM.createObjectNode();

        if (getConstraint() != null) {
          objectNode = OM.valueToTree(getConstraint());
        }

        objectNode.put("allow", isAllow());

        response = new OpaResponse().setResult(objectNode);
      }
      wireMockExtension.stubFor(mappingBuilder.willReturn(getResponse(response)));
    }
  }
}
