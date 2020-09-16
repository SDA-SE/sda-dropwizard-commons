package org.sdase.commons.server.opa.testing;

import static io.dropwizard.testing.ConfigOverride.config;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;
import static org.sdase.commons.server.opa.testing.OpaRule.onRequest;

import com.google.common.collect.Lists;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.health.PolicyExistsHealthCheck;
import org.sdase.commons.server.opa.testing.test.ConstraintModel;
import org.sdase.commons.server.opa.testing.test.OpaBundeTestAppConfiguration;
import org.sdase.commons.server.opa.testing.test.OpaBundleTestApp;
import org.sdase.commons.server.opa.testing.test.PrincipalInfo;
import org.sdase.commons.server.testing.LazyRule;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaIT {

  private static final OpaRule OPA_RULE = new OpaRule();

  private static final LazyRule<DropwizardAppRule<OpaBundeTestAppConfiguration>> DW =
      new LazyRule<>(
          () ->
              new DropwizardAppRule<>(
                  OpaBundleTestApp.class,
                  ResourceHelpers.resourceFilePath("test-opa-config.yaml"),
                  config("opa.baseUrl", OPA_RULE.getUrl())));

  @ClassRule public static final RuleChain chain = RuleChain.outerRule(OPA_RULE).around(DW);
  @Rule public RetryRule retryRule = new RetryRule();

  private static String path = "resources";
  private static String method = "GET";

  @Before
  public void before() {
    OPA_RULE.reset();
  }

  @Test
  @Retry(5)
  public void shouldAllowAccess() {
    // given
    OPA_RULE.mock(onRequest().withHttpMethod(method).withPath(path).allow());
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
  @Retry(5)
  public void shouldAllowAccessWithConstraints() {
    // given
    OPA_RULE.mock(
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
        .contains(
            entry("customer_ids", Lists.newArrayList("1", "2")),
            entry("agent_ids", Lists.newArrayList("A1")));
    assertThat(principalInfo.getConstraints().isFullAccess()).isFalse();
    assertThat(principalInfo.getJwt()).isNull();
  }

  @Test
  @Retry(5)
  public void shouldDenyAccess() {
    // given
    OPA_RULE.mock(onRequest().withHttpMethod(method).withPath(path).deny());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @Retry(5)
  public void shouldDenyAccessWithConstraints() {
    // given
    OPA_RULE.mock(
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
  @Retry(5)
  public void shouldDenyAccessIfOpaResponseIsBroken() {
    // given
    OPA_RULE.mock(onAnyRequest().serverError());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @Retry(5)
  public void shouldDenyAccessIfOpaResponseEmpty() {
    // given
    OPA_RULE.mock(onAnyRequest().emptyResponse());
    // when
    Response response = doGetRequest();
    // then
    assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @Retry(5)
  public void shouldAllowAccessForLongerPathAndPost() {
    // given
    String longerPath = "resources/actions";
    OPA_RULE.mock(onRequest().withHttpMethod("POST").withPath(longerPath).allow());
    // when
    Response response = doPostRequest(longerPath);
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    OPA_RULE.verify(1, "POST", longerPath);
  }

  @Test
  @Retry(5)
  public void shouldNotInvokeSwaggerUrls() {
    // given
    String excludedPath = "swagger.json";
    OPA_RULE.mock(onRequest().withHttpMethod("GET").withPath(excludedPath).allow());
    // when
    Response response = doGetRequest(excludedPath);
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    OPA_RULE.verify(0, "GET", excludedPath);
  }

  @Test
  @Retry(5)
  public void shouldNotInvokeOpenApiUrls() {
    // given
    String excludedPath = "openapi.json";
    OPA_RULE.mock(onRequest().withHttpMethod("GET").withPath(excludedPath).allow());
    // when
    Response response = doGetRequest(excludedPath);
    // then
    assertThat(response.getStatus()).isEqualTo(SC_OK);
    OPA_RULE.verify(0, "GET", excludedPath);
  }

  @Test
  @Retry(5)
  public void shouldIncludeHealthCheck() {
    Response response =
        DW.getRule()
            .client()
            .target("http://localhost:" + DW.getRule().getAdminPort()) // NOSONAR
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
    return DW.getRule()
        .client()
        .target("http://localhost:" + DW.getRule().getLocalPort())
        .path(rPath)
        .request()
        .get();
  }

  private Response doPostRequest(String rPath) {
    return DW.getRule()
        .client()
        .target("http://localhost:" + DW.getRule().getLocalPort())
        .path(rPath)
        .request()
        .post(Entity.json(null));
  }
}
