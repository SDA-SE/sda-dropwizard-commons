package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sdase.commons.client.jersey.test.SubtypesClient.AbstractResource.ResourceType.ONE;
import static org.sdase.commons.client.jersey.test.SubtypesClient.AbstractResource.ResourceType.TWO;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.UUID;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.SubtypesClient;
import org.sdase.commons.client.jersey.test.SubtypesClient.AbstractResource;
import org.sdase.commons.client.jersey.test.SubtypesClient.ResourceOne;
import org.sdase.commons.client.jersey.test.SubtypesClient.ResourceTwo;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockExtension;

class SubtypeMappingAndValidationTest {

  @RegisterExtension
  @Order(0)
  final WireMockExtension wire = new WireMockExtension();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<ClientTestConfig> dw =
      new DropwizardAppExtension<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  @Test
  void shouldGetValidMainResourceOne() {
    stubTypeResponse("ONE");
    AbstractResource resource = subtypesClient().getOnlyValidSubtype();
    assertThat(resource)
        .isInstanceOf(ResourceOne.class)
        .extracting(AbstractResource::getType)
        .isEqualTo(ONE);
  }

  @Test
  void shouldGetValidMainResourceTwoWithNested() {
    stubNestedTypeResponse("ONE", "TWO");
    AbstractResource resource = subtypesClient().getOnlyValidSubtype();
    assertThat(resource)
        .isInstanceOf(ResourceTwo.class)
        .extracting(AbstractResource::getType)
        .isEqualTo(TWO);
    assertThat((ResourceTwo) resource)
        .extracting(ResourceTwo::getNested)
        .isExactlyInstanceOf(ResourceOne.class);
    assertThat((ResourceTwo) resource).extracting(ResourceTwo::getAnyType).isEqualTo(TWO);
  }

  @Test
  void shouldFailOnUnknownNestedType() {
    stubNestedTypeResponse("THREE", "ONE");
    SubtypesClient subtypesClient = subtypesClient();
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(subtypesClient::getOnlyValidSubtype);
  }

  @Test
  void shouldFailOnUnknownEnumProperty() {
    stubNestedTypeResponse("ONE", "THREE");
    SubtypesClient subtypesClient = subtypesClient();
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(subtypesClient::getOnlyValidSubtype);
  }

  @Test
  void shouldNotFailOnInvalidResourceWithoutNestedWhenFaultTolerant() {
    stubNestedTypeResponse("THREE", "TWO");
    AbstractResource resource = subtypesClient().getSubtype();
    assertThat(resource)
        .isInstanceOf(ResourceTwo.class)
        .extracting(AbstractResource::getType)
        .isEqualTo(TWO);
    assertThat((ResourceTwo) resource).extracting(ResourceTwo::getNested).isNull();
    assertThat((ResourceTwo) resource).extracting(ResourceTwo::getAnyType).isEqualTo(TWO);
  }

  @Test
  void shouldNotFailOnInvalidResourceWithoutEnumWhenFaultTolerant() {
    stubNestedTypeResponse("ONE", "THREE");
    SubtypesClient subtypesClient = subtypesClient();
    AbstractResource resource = subtypesClient.getSubtype();
    assertThat(resource)
        .isInstanceOf(ResourceTwo.class)
        .extracting(AbstractResource::getType)
        .isEqualTo(TWO);
    assertThat((ResourceTwo) resource)
        .extracting(ResourceTwo::getNested)
        .isExactlyInstanceOf(ResourceOne.class);
    assertThat((ResourceTwo) resource).extracting(ResourceTwo::getAnyType).isNull();
  }

  @Test
  void shouldFailOnInvalidResourceType() {
    stubTypeResponse("THREE");
    SubtypesClient subtypesClient = subtypesClient();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(subtypesClient::getOnlyValidSubtype);
  }

  @Test
  void shouldNotFailOnInvalidResourceTypeWhenFaultTolerant() {
    stubTypeResponse("THREE");
    AbstractResource resource = subtypesClient().getSubtype();
    assertThat(resource).isNull();
  }

  private void stubTypeResponse(String type) {
    wire.stubFor(
        get("/subtypes")
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody("{\"type\": \"" + type + "\"}")));
  }

  private void stubNestedTypeResponse(String nestedType, String anyType) {
    wire.stubFor(
        get("/subtypes")
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody(
                        "{\"type\": \"TWO\", "
                            + "\"nested\": {\"type\": \""
                            + nestedType
                            + "\"}, \"anyType\": \""
                            + anyType
                            + "\"}")));
  }

  private SubtypesClient subtypesClient() {
    ClientTestApp app = dw.getApplication();
    return app.getJerseyClientBundle()
        .getClientFactory()
        .platformClient()
        .api(SubtypesClient.class, "SubtypesClient-" + UUID.randomUUID())
        .atTarget(wire.baseUrl());
  }
}
