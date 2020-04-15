package org.sdase.commons.server.opa.testing;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;
import static org.sdase.commons.server.opa.testing.OpaRule.onRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.VerificationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sdase.commons.server.opa.filter.model.OpaInput;
import org.sdase.commons.server.opa.filter.model.OpaRequest;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaRuleIT {

  @ClassRule public static final OpaRule OPA_RULE = new OpaRule();
  @Rule public RetryRule rule = new RetryRule();

  private String path = "resources";
  private String method = "GET";
  private String jwt = "aaaa.bbbb.cccc";

  @Before
  public void before() {
    OPA_RULE.reset();
  }

  @Test
  @Retry(5)
  public void shouldReturnResponseOnMatchWithLegacyOnRequest() {
    // given
    OPA_RULE.mock(onRequest(method, path).allow());
    // when
    Response response = requestMock(request(method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
    OPA_RULE.verify(1, method, path);
  }

  @Test
  @Retry(5)
  public void shouldReturnResponseOnMatch() {
    // given
    OPA_RULE.mock(onRequest().withHttpMethod(method).withPath(path).withJwt(jwt).allow());
    // when
    Response response = requestMock(requestWithJwt(jwt, method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
    OPA_RULE.verify(1, onRequest().withHttpMethod(method).withPath(path).withJwt(jwt));
  }

  @Test(expected = VerificationException.class)
  public void shouldThrowExceptionIfNotVerified() {
    OPA_RULE.verify(1, method, path);
  }

  @Test(expected = VerificationException.class)
  public void shouldThrowExceptionIfUnexpectedToken() {
    // given
    OPA_RULE.mock(onRequest().withHttpMethod(method).withPath(path).withJwt(jwt).allow());
    // when
    requestMock(requestWithJwt("INVALID", method, path));
    // then
    OPA_RULE.verify(1, onRequest().withHttpMethod(method).withPath(path).withJwt(jwt));
  }

  @Test
  @Retry(5)
  public void shouldReturnServerError() {
    // given
    OPA_RULE.mock(onAnyRequest().serverError());
    // when
    Response response = requestMock(request(method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  @Retry(5)
  public void shouldReturnEmptyResponse() {
    // given
    OPA_RULE.mock(onAnyRequest().emptyResponse());
    // when
    Response response = requestMock(request(method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(String.class)).isNullOrEmpty();
  }

  @Test
  @Retry(5)
  public void shouldMockLongPathSuccessfully() {
    // given
    OPA_RULE.mock(onRequest().withHttpMethod("GET").withPath("/p1/p2").allow());

    // when
    Response response = requestMock(request("GET", "p1", "p2"));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
  }

  @Test
  @Retry(5)
  public void shouldMockLongPathWithTrailingSlashSuccessfully() {
    // given
    OPA_RULE.mock(onRequest().withHttpMethod("GET").withPath("/p1/p2//").allow());

    // when
    Response response = requestMock(request("GET", "p1", "p2"));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
  }

  @Test
  @Retry(5)
  public void shouldReturnDifferentResponsesInOneTest() {
    // given
    ConstraintModel constraintModel = new ConstraintModel().addConstraint("key", "A", "B");
    OPA_RULE.mock(onRequest("GET", "pathA").allow().withConstraint(constraintModel));
    OPA_RULE.mock(onRequest("POST", "pathB").deny());
    OPA_RULE.mock(
        onRequest()
            .withHttpMethod("POST")
            .withPath("pathC")
            .withJwt(jwt)
            .allow()
            .withConstraint(constraintModel));
    OPA_RULE.mock(onRequest().withHttpMethod("POST").withPath("pathD").withJwt(null).deny());
    // when
    Response response = requestMock(request("GET", "pathA"));
    Response response2 = requestMock(request("POST", "pathB"));
    Response response3 = requestMock(requestWithJwt(jwt, "POST", "pathC"));
    Response response4 = requestMock(request("POST", "pathD"));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response2.getStatus()).isEqualTo(SC_OK);
    OpaResponse opaResponse = response.readEntity(OpaResponse.class);
    assertThat(opaResponse.isAllow()).isTrue();
    OpaResponse opaResponse2 = response2.readEntity(OpaResponse.class);
    assertThat(opaResponse2.isAllow()).isFalse();
    OpaResponse opaResponse3 = response3.readEntity(OpaResponse.class);
    assertThat(opaResponse3.isAllow()).isTrue();
    OpaResponse opaResponse4 = response4.readEntity(OpaResponse.class);
    assertThat(opaResponse4.isAllow()).isFalse();
    OPA_RULE.verify(1, onRequest().withHttpMethod("POST").withPath("pathC").withJwt(jwt));
    OPA_RULE.verify(1, onRequest().withHttpMethod("POST").withPath("pathD").withJwt(null));
  }

  @Test()
  public void shouldSplitPath() {
    assertThat(OpaRule.StubBuilder.splitPath("a")).containsExactly("a");
    assertThat(OpaRule.StubBuilder.splitPath("b/c")).containsExactly("b", "c");
    assertThat(OpaRule.StubBuilder.splitPath("/d")).containsExactly("d");
    assertThat(OpaRule.StubBuilder.splitPath("/e/f/")).containsExactly("e", "f");
    assertThat(OpaRule.StubBuilder.splitPath("/g/h/i/j////")).containsExactly("g", "h", "i", "j");
  }

  private Response requestMock(OpaRequest request) {
    return JerseyClientBuilder.createClient()
        .target(OPA_RULE.getUrl())
        .path("/v1/data/policy")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .post(Entity.json(request));
  }

  private OpaRequest request(String method, String... path) {
    return requestWithJwt(null, method, path);
  }

  private OpaRequest requestWithJwt(String jwt, String method, String... path) {
    ObjectMapper objectMapper = new ObjectMapper();
    return new OpaRequest()
        .setInput(
            objectMapper.valueToTree(
                new OpaInput().setJwt(jwt).setHttpMethod(method).setPath(path)));
  }
}
