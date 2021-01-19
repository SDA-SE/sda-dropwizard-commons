package org.sdase.commons.server.opa.testing;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.sdase.commons.server.opa.testing.OpaClassExtension.onAnyRequest;
import static org.sdase.commons.server.opa.testing.OpaClassExtension.onRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.VerificationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.filter.model.OpaInput;
import org.sdase.commons.server.opa.filter.model.OpaRequest;
import org.sdase.commons.server.opa.filter.model.OpaResponse;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;

class OpaClassExtensionIT {

  @RegisterExtension static final OpaClassExtension OPA_EXTENSION = new OpaClassExtension();

  private final String path = "resources";
  private final String method = "GET";
  private final String jwt = "aaaa.bbbb.cccc";

  @BeforeEach
  void before() {
    OPA_EXTENSION.reset();
  }

  @RetryingTest(5)
  void shouldReturnResponseOnMatchWithLegacyOnRequest() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).allow());
    // when
    Response response = requestMock(request(method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
    OPA_EXTENSION.verify(1, method, path);
  }

  @RetryingTest(5)
  void shouldReturnResponseOnMatch() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).withJwt(jwt).allow());
    // when
    Response response = requestMock(requestWithJwt(jwt, method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
    OPA_EXTENSION.verify(1, onRequest().withHttpMethod(method).withPath(path).withJwt(jwt));
  }

  @Test
  void shouldThrowExceptionIfNotVerified() {
    assertThrows(VerificationException.class, () -> OPA_EXTENSION.verify(1, method, path));
  }

  @Test
  void shouldThrowExceptionIfUnexpectedToken() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).withJwt(jwt).allow());
    // when
    requestMock(requestWithJwt("INVALID", method, path));
    AbstractOpa.RequestExtraBuilder requestExtraBuilder =
        onRequest().withHttpMethod(method).withPath(path).withJwt(jwt);
    // then

    assertThrows(
        VerificationException.class,
        () -> {
          OPA_EXTENSION.verify(1, requestExtraBuilder);
        });
  }

  @RetryingTest(5)
  void shouldReturnServerError() {
    // given
    OPA_EXTENSION.mock(onAnyRequest().serverError());
    // when
    Response response = requestMock(request(method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
  }

  @RetryingTest(5)
  void shouldReturnEmptyResponse() {
    // given
    OPA_EXTENSION.mock(onAnyRequest().emptyResponse());
    // when
    Response response = requestMock(request(method, path));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(String.class)).isNullOrEmpty();
  }

  @RetryingTest(5)
  void shouldMockLongPathSuccessfully() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod("GET").withPath("/p1/p2").allow());

    // when
    Response response = requestMock(request("GET", "p1", "p2"));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
  }

  @RetryingTest(5)
  void shouldMockLongPathWithTrailingSlashSuccessfully() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod("GET").withPath("/p1/p2//").allow());

    // when
    Response response = requestMock(request("GET", "p1", "p2"));
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    assertThat(response.readEntity(OpaResponse.class).isAllow()).isTrue();
  }

  @RetryingTest(5)
  void shouldReturnDifferentResponsesInOneTest() {
    // given
    ConstraintModel constraintModel = new ConstraintModel().addConstraint("key", "A", "B");
    OPA_EXTENSION.mock(
        onRequest()
            .withHttpMethod("GET")
            .withPath("pathA")
            .allow()
            .withConstraint(constraintModel));
    OPA_EXTENSION.mock(onRequest().withHttpMethod("POST").withPath("pathB").deny());
    OPA_EXTENSION.mock(
        onRequest()
            .withHttpMethod("POST")
            .withPath("pathC")
            .withJwt(jwt)
            .allow()
            .withConstraint(constraintModel));
    OPA_EXTENSION.mock(onRequest().withHttpMethod("POST").withPath("pathD").withJwt(null).deny());
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
    OPA_EXTENSION.verify(1, onRequest().withHttpMethod("POST").withPath("pathC").withJwt(jwt));
    OPA_EXTENSION.verify(1, onRequest().withHttpMethod("POST").withPath("pathD").withJwt(null));
  }

  @Test
  void shouldSplitPath() {
    assertThat(OpaClassExtension.StubBuilder.splitPath("a")).containsExactly("a");
    assertThat(OpaClassExtension.StubBuilder.splitPath("b/c")).containsExactly("b", "c");
    assertThat(OpaClassExtension.StubBuilder.splitPath("/d")).containsExactly("d");
    assertThat(OpaClassExtension.StubBuilder.splitPath("/e/f/")).containsExactly("e", "f");
    assertThat(OpaClassExtension.StubBuilder.splitPath("/g/h/i/j////"))
        .containsExactly("g", "h", "i", "j");
  }

  private Response requestMock(OpaRequest request) {
    return JerseyClientBuilder.createClient()
        .target(OPA_EXTENSION.getUrl())
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
