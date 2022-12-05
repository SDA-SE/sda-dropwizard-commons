package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.util.Lists.newArrayList;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onAnyRequest;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.test.AuthAndOpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.AuthAndOpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;

class AuthAndOpaIT {

  @RegisterExtension
  @Order(0)
  private static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @RegisterExtension
  @Order(1)
  private static final OpaClassExtension OPA_EXTENSION = new OpaClassExtension();

  @RegisterExtension
  @Order(2)
  private static final DropwizardAppExtension<AuthAndOpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          AuthAndOpaBundleTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA_EXTENSION::getUrl));

  private static final String path = "resources";
  private static final String method = "GET";
  private String jwt;

  @BeforeEach
  void before() {
    jwt = AUTH.auth().buildToken();
    OPA_EXTENSION.reset();
  }

  @Test
  @RetryingTest(5)
  void shouldNotAccessSimpleWithInvalidToken() {
    Response response =
        getResources(
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getHeaderString(WWW_AUTHENTICATE)).contains("Bearer");
    assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
    assertThat(response.readEntity(new GenericType<Map<String, Object>>() {}))
        .containsKeys("title", "invalidParams");
    OPA_EXTENSION.verify(0, onAnyRequest());
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessSimple() {
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).withJwt(jwt).allow());

    Response response = getResources(true);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
    assertThat(principalInfo.getJwt()).isEqualTo(jwt);
    assertThat(principalInfo.getConstraints().getConstraint()).isNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessConstraints() {
    OPA_EXTENSION.mock(
        onRequest()
            .withHttpMethod(method)
            .withPath(path)
            .withJwt(jwt)
            .allow()
            .withConstraint(
                new ConstraintModel().setFullAccess(true).addConstraint("customer_ids", "1")));

    Response response = getResources(true);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
    assertThat(principalInfo.getName()).isEqualTo("OpaJwtPrincipal");
    assertThat(principalInfo.getJwt()).isNotNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isTrue();
    assertThat(principalInfo.getConstraints().getConstraint())
        .contains(entry("customer_ids", newArrayList("1")));
    assertThat(principalInfo.getConstraintsJson()).contains("\"customer_ids\":").contains("\"1\"");
    assertThat(principalInfo.getSub()).isEqualTo("test");
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessWithoutTokenSimple() {
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).withJwt(null).allow());

    Response response = getResources(false);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
    assertThat(principalInfo.getJwt()).isNull();
    assertThat(principalInfo.getConstraints().getConstraint()).isNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessWithoutTokenConstraints() {
    OPA_EXTENSION.mock(
        onRequest()
            .withHttpMethod(method)
            .withPath(path)
            .withJwt(null)
            .allow()
            .withConstraint(
                new ConstraintModel().setFullAccess(true).addConstraint("customer_ids", "1")));

    Response response = getResources(false);

    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
    assertThat(principalInfo.getJwt()).isNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isTrue();
    assertThat(principalInfo.getConstraints().getConstraint())
        .contains(entry("customer_ids", newArrayList("1")));
  }

  private Response getResources(boolean withJwt) {
    return getResources(withJwt ? jwt : null);
  }

  private Response getResources(String jwt) {
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    if (jwt != null) {
      headers.put(HttpHeaders.AUTHORIZATION, newArrayList("Bearer " + jwt));
    }

    return DW.client()
        .target("http://localhost:" + DW.getLocalPort()) // NOSONAR
        .path(path)
        .request()
        .headers(headers)
        .get();
  }
}
