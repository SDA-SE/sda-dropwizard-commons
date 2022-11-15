package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.error.ClientErrorUtil;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

class ClientErrorUtilTest {

  @RegisterExtension
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  private static final ObjectMapper OM = new ObjectMapper();

  private WebTarget webTarget;

  @BeforeEach
  void resetRequests() {
    WIRE.resetAll();
    webTarget = JerseyClientBuilder.createClient().target(WIRE.baseUrl());
  }

  @Test
  void catchNotFoundException() {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader(
                        CONTENT_TYPE,
                        TEXT_PLAIN) // https://github.com/dropwizard/dropwizard/issues/231
                    .withBody("Not Found")));

    assertThatExceptionOfType(ClientRequestException.class)
        .isThrownBy(
            () ->
                ClientErrorUtil.convertExceptions(
                    () -> webTarget.request(APPLICATION_JSON).get(String.class)))
        .withCauseInstanceOf(NotFoundException.class);
  }

  @Test
  void passThroughTheResponseEntity() throws JsonProcessingException {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(
                        OM.writeValueAsBytes(
                            singletonMap("message", "This has been found"))) // NOSONAR
                ));

    Map<String, String> actual =
        ClientErrorUtil.convertExceptions(
            () ->
                webTarget.request(APPLICATION_JSON).get(new GenericType<Map<String, String>>() {}));
    assertThat(actual).containsExactly(entry("message", "This has been found"));
  }

  @Test
  void readErrorEntityWithJacksonReference() throws JsonProcessingException {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(
                        OM.writeValueAsBytes(
                            asList("Error number one", "Error number two"))) // NOSONAR
                ));

    Object responseBody = null;
    List<String> errors = null;
    try {
      responseBody =
          ClientErrorUtil.convertExceptions(
              () -> webTarget.request(APPLICATION_JSON).get(String.class));
    } catch (ClientRequestException e) {
      errors = ClientErrorUtil.readErrorBody(e, new TypeReference<List<String>>() {});
    }
    assertThat(errors).isNotNull().containsExactly("Error number one", "Error number two");
    //noinspection ConstantConditions
    assertThat(responseBody).isNull();
  }

  @Test
  void readErrorEntityWithJaxRsReference() throws JsonProcessingException {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(
                        OM.writeValueAsBytes(asList("Error number one", "Error number two")))));

    Object responseBody = null;
    List<String> errors = null;
    try {
      responseBody =
          ClientErrorUtil.convertExceptions(
              () -> webTarget.request(APPLICATION_JSON).get(String.class));
    } catch (ClientRequestException e) {
      errors = ClientErrorUtil.readErrorBody(e, new GenericType<List<String>>() {});
    }
    assertThat(errors).isNotNull().containsExactly("Error number one", "Error number two");
    //noinspection ConstantConditions
    assertThat(responseBody).isNull();
  }

  @Test
  void readErrorEntityWithClassReference() {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader(
                        CONTENT_TYPE,
                        TEXT_PLAIN) // https://github.com/dropwizard/dropwizard/issues/231
                    .withBody("Error number one\nError number two")));

    Object responseBody = null;
    String errors = null;
    try {
      responseBody =
          ClientErrorUtil.convertExceptions(
              () -> webTarget.request(APPLICATION_JSON).get(String.class));
    } catch (ClientRequestException e) {
      errors = ClientErrorUtil.readErrorBody(e, String.class);
    }
    assertThat(errors).isEqualTo("Error number one\nError number two");
    assertThat(responseBody).isNull();
  }

  @Test
  void readDefaultErrorEntityFromException() throws JsonProcessingException {
    ApiError givenError =
        new ApiError(
            "An error for testing", // NOSONAR
            asList(
                new ApiInvalidParam("name", "Must not be null", "NOT_NULL"), // NOSONAR
                new ApiInvalidParam("type", "Must not be empty", "NOT_EMPTY") // NOSONAR
                ));
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(422)
                    .withHeader(
                        CONTENT_TYPE,
                        APPLICATION_JSON) // https://github.com/dropwizard/dropwizard/issues/231
                    .withBody(OM.writeValueAsBytes(givenError))));

    String responseBody = null;
    ApiError errors = null;
    try {
      responseBody =
          ClientErrorUtil.convertExceptions(
              () -> webTarget.request(APPLICATION_JSON).get(String.class));
    } catch (ClientRequestException e) {
      errors = ClientErrorUtil.readErrorBody(e);
    }
    assertThat(errors).isNotNull();
    assertThat(errors.getTitle()).isEqualTo("An error for testing"); // NOSONAR
    assertThat(errors.getInvalidParams())
        .extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode)
        .containsExactly(
            tuple("name", "Must not be null", "NOT_NULL"),
            tuple("type", "Must not be empty", "NOT_EMPTY"));
    assertThat(responseBody).isNull();
  }

  @Test
  void readDefaultErrorEntityFromResponse() throws JsonProcessingException {
    ApiError givenError =
        new ApiError(
            "An error for testing",
            asList(
                new ApiInvalidParam("name", "Must not be null", "NOT_NULL"),
                new ApiInvalidParam("type", "Must not be empty", "NOT_EMPTY")));
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(422)
                    .withHeader(
                        CONTENT_TYPE,
                        APPLICATION_JSON) // https://github.com/dropwizard/dropwizard/issues/231
                    .withBody(OM.writeValueAsBytes(givenError))));

    Response response = webTarget.request(APPLICATION_JSON).get();
    ApiError errors = ClientErrorUtil.readErrorBody(response);
    assertThat(errors).isNotNull();
    assertThat(errors.getTitle()).isEqualTo("An error for testing"); // NOSONAR
    assertThat(errors.getInvalidParams())
        .extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode)
        .containsExactly(
            tuple("name", "Must not be null", "NOT_NULL"),
            tuple("type", "Must not be empty", "NOT_EMPTY"));
  }

  @Test
  void readNoErrorEntityAsTypeReferenceFromSuccessfulResponse() throws JsonProcessingException {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(
                        OM.writeValueAsBytes(singletonMap("message", "This has been found")))));

    Response response = webTarget.request(APPLICATION_JSON).get();
    Map<String, Object> errorBody =
        ClientErrorUtil.readErrorBody(response, new TypeReference<Map<String, Object>>() {});
    assertThat(errorBody).isNull();
  }

  @Test
  void readNoErrorEntityAsGenericTypeFromSuccessfulResponse() throws JsonProcessingException {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(APPLICATION_JSON))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .withBody(
                        OM.writeValueAsBytes(singletonMap("message", "This has been found")))));

    Response response = webTarget.request(APPLICATION_JSON).get();
    Map<String, Object> errorBody =
        ClientErrorUtil.readErrorBody(response, new GenericType<Map<String, Object>>() {});
    assertThat(errorBody).isNull();
  }

  @Test
  void readNoErrorEntityAsClassFromSuccessfulResponse() {
    WIRE.stubFor(
        get("/")
            .withHeader(ACCEPT, equalTo(TEXT_PLAIN))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader(
                        CONTENT_TYPE,
                        TEXT_PLAIN) // https://github.com/dropwizard/dropwizard/issues/231
                    .withBody("This has been found")));

    Response response = webTarget.request(TEXT_PLAIN).get();
    String errorBody = ClientErrorUtil.readErrorBody(response, String.class);
    assertThat(errorBody).isNull();
  }
}
