package org.sdase.commons.server.opa.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class AbstractOpa {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractOpa.class);

  private static final ObjectMapper OM = new ObjectMapper();

  public RequestPatternBuilder buildRequestPattern(AbstractStubBuilder builder) {
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

  public interface FinalBuilder<T> extends BuildBuilder {

    FinalBuilder withConstraint(Object c);
  }

  public interface BuildBuilder<T> {
    void build(T wire);
  }

  public abstract static class AbstractStubBuilder
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

    AbstractStubBuilder onAnyRequest() {
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

    public AbstractStubBuilder allow() {
      this.allow = true;
      return this;
    }

    public AbstractStubBuilder deny() {
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
