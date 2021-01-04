package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockExtension;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class OpaExtension implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOG = LoggerFactory.getLogger(OpaExtension.class);

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

  private static String getJson(String[] paths) {
    try {
      return OM.writeValueAsString(paths);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Mock initialization failed");
    }
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

  @Override
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
    verify(count, (StubBuilder) allowBuilder);
  }

  private void verify(int count, StubBuilder builder) {
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

    wire.verify(count, requestPattern);
  }

  public void mock(BuildBuilder builder) {
    builder.build(wire);
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
    void build(WireMockExtension wire);
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
    private boolean allow;
    private boolean onAnyRequest = false;
    private boolean errorResponse = false;
    private boolean emptyResponse = false;
    private OpaResponse answer;
    private Object constraint;

    private static ResponseDefinitionBuilder getResponse(OpaResponse response) {
      try {
        return aResponse()
            .withStatus(200)
            .withHeader("Content-type", "application/json")
            .withBody(OM.writeValueAsBytes(response));
      } catch (JsonProcessingException exception) {
        throw new IllegalStateException("Mock initialization failed");
      }
    }

    private StubBuilder onAnyRequest() {
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

    @Override
    public void build(WireMockExtension wire) {
      MappingBuilder mappingBuilder;
      if (onAnyRequest) {
        mappingBuilder = matchAnyPostUrl();
      } else {
        mappingBuilder = matchInput(httpMethod, paths);

        if (matchJWT) {
          mappingBuilder.withRequestBody(
              matchingJsonPath("$.input.jwt", jwt != null ? equalTo(jwt) : absent()));
        }
      }

      if (errorResponse) {
        wire.stubFor(mappingBuilder.willReturn(aResponse().withStatus(500)));
        return;
      }

      if (emptyResponse) {
        wire.stubFor(mappingBuilder.willReturn(null));
        return;
      }

      OpaResponse response;
      if (answer != null) {
        response = answer;
      } else {
        ObjectNode objectNode = OM.createObjectNode();

        if (constraint != null) {
          objectNode = OM.valueToTree(constraint);
        }

        objectNode.put("allow", allow);

        response = new OpaResponse().setResult(objectNode);
      }
      wire.stubFor(mappingBuilder.willReturn(getResponse(response)));
    }

    private MappingBuilder matchAnyPostUrl() {
      return post(urlMatching("/.*"));
    }

    private MappingBuilder matchInput(String httpMethod, String[] paths) {
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
