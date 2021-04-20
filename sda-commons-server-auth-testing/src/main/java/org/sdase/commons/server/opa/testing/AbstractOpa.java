package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.MultivaluedMap;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOpa {

  /**
   * Tests are a bit flaky on servers when OPA is involved. That's why we increase the default
   * timeout.
   */
  static final String OPA_CLIENT_TIMEOUT = "dw.opa.opaClient.timeout";

  static final String OPA_CLIENT_TIMEOUT_DEFAULT = "1000ms";

  private static final Logger LOG = LoggerFactory.getLogger(AbstractOpa.class);

  private static final ObjectMapper OM = new ObjectMapper();

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

  abstract void verify(int count, StubBuilder builder);

  public RequestPatternBuilder buildRequestPattern(StubBuilder builder) {
    RequestPatternBuilder requestPattern;

    if (builder.onAnyRequest) {
      requestPattern =
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching("/.*"));
    } else {
      requestPattern =
          RequestPatternBuilder.newRequestPattern(RequestMethod.POST, urlMatching("/.*"))
              .withRequestBody(matchingJsonPath("$.input.httpMethod", equalTo(builder.httpMethod)))
              .withRequestBody(
                  matchingJsonPath("$.input.path", equalToJson(getJson(builder.paths))));

      if (builder.matchJWT) {
        requestPattern.withRequestBody(
            matchingJsonPath("$.input.jwt", builder.jwt != null ? equalTo(builder.jwt) : absent()));
      }
    }
    return requestPattern;
  }

  static String getJson(String[] paths) {
    try {
      return OM.writeValueAsString(paths);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Mock initialization failed");
    }
  }

  public interface RequestMethodBuilder {
    RequestPathBuilder withHttpMethod(String httpMethod);
  }

  public interface RequestPathBuilder {
    RequestExtraBuilder withPath(String path);
  }

  public interface RequestExtraBuilder extends AllowBuilder {
    RequestExtraBuilder withJwt(String jwt);

    default RequestExtraBuilder withJwtFromHeaderValue(String jwtWithBearerPrefix) {
      if (jwtWithBearerPrefix == null || !jwtWithBearerPrefix.toLowerCase().startsWith("bearer ")) {
        LOG.warn(
            "Requested to mock OPA request from header value but no Bearer prefix found in {}",
            jwtWithBearerPrefix);
        return this;
      }
      return withJwt(jwtWithBearerPrefix.substring("bearer".length()).trim());
    }

    default RequestExtraBuilder withJwtFromHeaders(MultivaluedMap<String, Object> headers) {
      String jwt =
          headers.keySet().stream()
              .filter(Objects::nonNull)
              .filter("Authorization"::equalsIgnoreCase)
              .map(headers::get)
              .flatMap(List::stream)
              .filter(Objects::nonNull)
              .map(Object::toString)
              .filter(authValue -> authValue.toLowerCase().startsWith("bearer "))
              .map(authValueWithBearer -> authValueWithBearer.substring("bearer".length()).trim())
              .findFirst()
              .orElse(null);
      if (jwt != null) {
        return withJwt(jwt);
      } else {
        LOG.warn("Requested to mock OPA with JWT from headers but no JWT was found in {}", headers);
      }
      return this;
    }
  }

  public interface AllowBuilder {
    FinalBuilder allow();

    FinalBuilder deny();

    BuildBuilder answer(OpaResponse response);

    BuildBuilder emptyResponse();

    BuildBuilder serverError();
  }

  public interface FinalBuilder extends BuildBuilder {

    FinalBuilder withConstraint(Object c);
  }

  public interface BuildBuilder {
    void build(WireMockServer wire);
  }

  public static class StubBuilder
      implements RequestMethodBuilder,
          RequestPathBuilder,
          RequestExtraBuilder,
          AllowBuilder,
          FinalBuilder,
          BuildBuilder {

    private String httpMethod;
    private String[] paths;
    private boolean matchJWT;
    private String jwt;

    @Override
    public void build(WireMockServer wire) {

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
        wire.stubFor(mappingBuilder.willReturn(aResponse().withStatus(500)));
        return;
      }

      if (isEmptyResponse()) {
        wire.stubFor(mappingBuilder.willReturn(null));
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
      wire.stubFor(mappingBuilder.willReturn(getResponse(response)));
    }

    public boolean isAllow() {
      return allow;
    }

    private boolean allow;

    public String getHttpMethod() {
      return httpMethod;
    }

    public String[] getPaths() {
      return paths;
    }

    public boolean isMatchJWT() {
      return matchJWT;
    }

    public String getJwt() {
      return jwt;
    }

    public boolean isOnAnyRequest() {
      return onAnyRequest;
    }

    private boolean onAnyRequest = false;
    private boolean errorResponse = false;

    public boolean isErrorResponse() {
      return errorResponse;
    }

    public boolean isEmptyResponse() {
      return emptyResponse;
    }

    public OpaResponse getAnswer() {
      return answer;
    }

    private boolean emptyResponse = false;
    private OpaResponse answer;

    public Object getConstraint() {
      return constraint;
    }

    private Object constraint;

    static ResponseDefinitionBuilder getResponse(OpaResponse response) {
      try {
        return aResponse()
            .withStatus(200)
            .withHeader("Content-type", "application/json")
            .withBody(OM.writeValueAsBytes(response));
      } catch (JsonProcessingException exception) {
        throw new IllegalStateException("Mock initialization failed");
      }
    }

    StubBuilder onAnyRequest() {
      onAnyRequest = true;
      return this;
    }

    public RequestPathBuilder withHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
      return this;
    }

    public RequestExtraBuilder withPath(String path) {
      this.paths = splitPath(path);
      return this;
    }

    public RequestExtraBuilder withJwt(String jwt) {
      matchJWT = true;
      this.jwt = jwt;
      return this;
    }

    public StubBuilder allow() {
      this.allow = true;
      return this;
    }

    public StubBuilder deny() {
      this.allow = false;
      return this;
    }

    @Override
    public BuildBuilder answer(OpaResponse answer) {
      this.answer = answer;
      return this;
    }

    @Override
    public BuildBuilder emptyResponse() {
      this.emptyResponse = true;
      return this;
    }

    @Override
    public BuildBuilder serverError() {
      this.errorResponse = true;
      return this;
    }

    @Override
    public FinalBuilder withConstraint(Object c) {
      this.constraint = c;
      return this;
    }

    MappingBuilder matchAnyPostUrl() {
      return post(urlMatching("/.*"));
    }

    MappingBuilder matchInput(String httpMethod, String[] paths) {
      return matchAnyPostUrl()
          .withRequestBody(matchingJsonPath("$.input.httpMethod", equalTo(httpMethod)))
          .withRequestBody(matchingJsonPath("$.input.path", equalToJson(getJson(paths))));
    }

    static String[] splitPath(String path) {
      if (path.startsWith("/")) {
        path = path.substring(1);
      }
      return path.split("/");
    }
  }
}
