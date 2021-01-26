package org.sdase.commons.starter.example;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onRequest;
import static org.sdase.commons.server.opa.testing.OpaRule.onAnyRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.openapitools.jackson.dataformat.hal.HALLink;
import java.util.List;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.OpaClassExtension;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import org.sdase.commons.starter.SdaPlatformConfiguration;
import org.sdase.commons.starter.example.people.db.PersonEntity;
import org.sdase.commons.starter.example.people.db.TestDataUtil;
import org.sdase.commons.starter.example.people.rest.PersonResource;

class SdaPlatformExampleApplicationIT {

  // create a dummy authentication provider that works as a local OpenId Connect provider for the
  // tests
  @Order(0)
  @RegisterExtension
  static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @Order(1)
  @RegisterExtension
  static final OpaClassExtension OPA = new OpaClassExtension();

  @Order(2)
  @RegisterExtension
  static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          SdaPlatformExampleApplication.class,
          // use the config file 'test-config.yaml' from the test resources folder
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  private static final String TEST_CONSUMER_TOKEN = "test-consumer";

  @BeforeEach
  void setupTestData() {
    TestDataUtil.clearTestData();
    PersonEntity john = TestDataUtil.addPersonEntity("john-doe", "John", "Doe");
    PersonEntity jane = TestDataUtil.addPersonEntity("jane-doe", "Jane", "Doe");
    TestDataUtil.addPersonEntity("jasmine-doe", "Jasmine", "Doe", asList(john, jane));

    OPA.reset();
  }

  @Test
  void accessSwaggerWithoutAuthentication() {
    Response response = baseUrlWebTarget().path("openapi.json").request(APPLICATION_JSON).get();

    assertThat(response).extracting(Response::getStatus).isEqualTo(200);
  }

  @Test
  void rejectApiRequestWithoutAuthentication() {
    Response response =
        baseUrlWebTarget()
            .path("people")
            .request(APPLICATION_JSON) // NOSONAR
            .header(ConsumerTracing.TOKEN_HEADER, TEST_CONSUMER_TOKEN)
            .get();

    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void accessApiWithAuthenticationAndConsumerToken() {
    String authHeader = AUTH.auth().buildHeaderValue();
    OPA.mock(onAnyRequest().deny());
    OPA.mock(
        onRequest()
            .withHttpMethod(HttpMethod.GET)
            .withPath("people")
            .withJwtFromHeaderValue(authHeader)
            .allow());

    Response response =
        baseUrlWebTarget()
            .path("people")
            .request(APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .header(ConsumerTracing.TOKEN_HEADER, TEST_CONSUMER_TOKEN)
            .get();

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void respond401DueToInvalidJWT() {
    OPA.mock(onAnyRequest().deny());
    Response response =
        baseUrlWebTarget()
            .path("people")
            .request(APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer: invalidToken")
            .get();

    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void respond404ForUnknownPerson() {
    OPA.mock(onAnyRequest().allow());
    Response response =
        baseUrlWebTarget()
            .path("people")
            .path("jamie-doe")
            .request(APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, AUTH.auth().buildHeaderValue())
            .header(ConsumerTracing.TOKEN_HEADER, TEST_CONSUMER_TOKEN)
            .get();

    assertThat(response).extracting(Response::getStatus).isEqualTo(404);
  }

  @Test
  void provideSelfLinkInPersonResource() {
    OPA.mock(onAnyRequest().allow());
    PersonResource personResource =
        baseUrlWebTarget()
            .path("people")
            .path("john-doe")
            .request(APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, AUTH.auth().buildHeaderValue())
            .header(ConsumerTracing.TOKEN_HEADER, TEST_CONSUMER_TOKEN)
            .get(PersonResource.class);

    String expectedSelfUri = baseUrlWebTarget().getUri().toASCIIString() + "/people/john-doe";
    HALLink actualSelf = personResource.getSelfLink();
    assertThat(actualSelf).extracting(HALLink::getHref).isEqualTo(expectedSelfUri);
  }

  @Test
  void provideRelationLinksInPersonResource() {
    OPA.mock(onAnyRequest().allow());
    PersonResource personResource =
        baseUrlWebTarget()
            .path("people")
            .path("jasmine-doe")
            .request(APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, AUTH.auth().buildHeaderValue())
            .header(ConsumerTracing.TOKEN_HEADER, TEST_CONSUMER_TOKEN)
            .get(PersonResource.class);

    String expectedJohnUri = baseUrlWebTarget().getUri().toASCIIString() + "/people/john-doe";
    String expectedJaneUri = baseUrlWebTarget().getUri().toASCIIString() + "/people/jane-doe";
    List<HALLink> actualParentsLinks = personResource.getParentsLinks();
    assertThat(actualParentsLinks)
        .extracting(HALLink::getHref)
        .containsExactlyInAnyOrder(expectedJohnUri, expectedJaneUri);
    List<HALLink> actualChildrenLinks = personResource.getChildrenLinks();
    assertThat(actualChildrenLinks).isNullOrEmpty();
  }

  private WebTarget baseUrlWebTarget() {
    return DW.client().target(String.format("http://localhost:%d", DW.getLocalPort()));
  }
}
