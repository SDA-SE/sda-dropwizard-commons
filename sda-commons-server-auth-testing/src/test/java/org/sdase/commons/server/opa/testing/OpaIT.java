package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onAnyRequest;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;

class OpaIT {

  @RegisterExtension
  @Order(0)
  private static final OpaClassExtension OPA_EXTENSION = new OpaClassExtension();

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<OpaBundeTestAppConfiguration> DW =
      new DropwizardAppExtension<>(
          OpaBundleTestApp.class,
          resourceFilePath("test-opa-config.yaml"),
          config("opa.baseUrl", OPA_EXTENSION::getUrl));

  private static final String path = "resources";
  private static final String method = "GET";

  @BeforeEach
  void before() {
    OPA_EXTENSION.reset();
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccess() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).allow());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);
    assertThat(principalInfo.getConstraints().getConstraint()).isNull();
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
    assertThat(principalInfo.getJwt()).isNull();
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessWithConstraints() {
    // given
    OPA_EXTENSION.mock(
        onRequest()
            .withHttpMethod(method)
            .withPath(path)
            .allow()
            .withConstraint(
                new ConstraintModel()
                    .addConstraint("customer_ids", "1", "2")
                    .addConstraint("agent_ids", "A1")));

    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    PrincipalInfo principalInfo = response.readEntity(PrincipalInfo.class);

    assertThat(principalInfo.getConstraints().getConstraint())
        .contains(entry("customer_ids", asList("1", "2")), entry("agent_ids", singletonList("A1")));
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
    assertThat(principalInfo.getJwt()).isNull();
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccess() {
    // given
    OPA_EXTENSION.mock(onRequest().withHttpMethod(method).withPath(path).deny());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessWithConstraints() {
    // given
    OPA_EXTENSION.mock(
        onRequest()
            .withHttpMethod(method)
            .withPath(path)
            .deny()
            .withConstraint(new ConstraintModel().addConstraint("customer_is", "1")));
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessIfOpaResponseIsBroken() {
    // given
    OPA_EXTENSION.mock(onAnyRequest().serverError());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @RetryingTest(5)
  void shouldDenyAccessIfOpaResponseEmpty() {
    // given
    OPA_EXTENSION.mock(onAnyRequest().emptyResponse());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @RetryingTest(5)
  void shouldAllowAccessForLongerPathAndPost() {
    // given
    String longerPath = "resources/actions";
    OPA_EXTENSION.mock(onRequest().withHttpMethod("POST").withPath(longerPath).allow());
    // when
    Response response = doPostRequest(longerPath);
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    OPA_EXTENSION.verify(1, "POST", longerPath);
  }

  @Test
  @RetryingTest(5)
  void shouldNotInvokeOpenApiUrls() {
    // given
    String excludedPath = "openapi.json";
    OPA_EXTENSION.mock(onRequest().withHttpMethod("GET").withPath(excludedPath).allow());
    // when
    Response response = doGetRequest(excludedPath);
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    OPA_EXTENSION.verify(0, "GET", excludedPath);
  }

  @Test
  @RetryingTest(5)
  void shouldIncludeHealthCheck() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getAdminPort()) // NOSONAR
            .path("healthcheck")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();

    assertThat(response.getStatus()).isEqualTo(SC_INTERNAL_SERVER_ERROR);
    assertThat(response.readEntity(String.class)).contains(PolicyExistsHealthCheck.DEFAULT_NAME);
  }

  private Response doGetRequest() {
    return doGetRequest(path);
  }

  private Response doGetRequest(String rPath) {
    return DW.client().target("http://localhost:" + DW.getLocalPort()).path(rPath).request().get();
  }

  private Response doPostRequest(String rPath) {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path(rPath)
        .request()
        .post(Entity.json(null));
  }
}
