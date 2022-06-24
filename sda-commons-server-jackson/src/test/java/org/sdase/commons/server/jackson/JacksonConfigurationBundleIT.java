package org.sdase.commons.server.jackson;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.jackson.test.JacksonConfigurationTestApp;
import org.sdase.commons.server.jackson.test.NameSearchFilterResource;
import org.sdase.commons.server.jackson.test.NestedNestedResource;
import org.sdase.commons.server.jackson.test.NestedResource;
import org.sdase.commons.server.jackson.test.PersonResource;
import org.sdase.commons.server.jackson.test.PersonWithChildrenResource;
import org.sdase.commons.server.jackson.test.ValidationResource;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

class JacksonConfigurationBundleIT {

  private static final String VALIDATION_ERROR_MESSAGE = "Request parameters are not valid.";

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(JacksonConfigurationTestApp.class, null, randomPorts());

  // Validation and Error Tests
  @Test
  void shouldGetError() {
    try {
      DW.client()
          .target("http://localhost:" + DW.getLocalPort())
          .path("/exception")
          .request(MediaType.APPLICATION_JSON)
          .get(PersonResource.class);
    } catch (WebApplicationException e) {
      ApiError details = e.getResponse().readEntity(ApiError.class);
      assertThat(details)
          .extracting(
              ApiError::getTitle,
              d -> d.getInvalidParams().get(0).getField(),
              d -> d.getInvalidParams().get(0).getReason(),
              d -> d.getInvalidParams().get(0).getErrorCode())
          .containsExactly("Some exception", "parameter", null, "SOME_ERROR_CODE");
    }
  }

  // Validation and Error Tests
  @Test
  void shouldGenerateApiErrorForJaxRsNotFoundException() {
    testJaxRsException("NotFound", 404, emptyList());
  }

  @Test
  void shouldGenerateApiErrorForJaxRsBadRequestException() {
    testJaxRsException("BadRequest", 400, emptyList());
  }

  @Test
  void shouldGenerateApiErrorForJaxRsForbiddenException() {
    testJaxRsException("Forbidden", 403, emptyList());
  }

  @Test
  void shouldGenerateApiErrorForJaxRsNotAcceptableException() {
    testJaxRsException("NotAcceptable", 406, emptyList());
  }

  @Test
  void shouldGenerateApiErrorForJaxRsNotAllowedException() {
    testJaxRsException("NotAllowed", 405, Collections.singletonList("Allow"));
  }

  @Test
  void shouldGenerateApiErrorForJaxRsNotAuthorizedException() {
    testJaxRsException("NotAuthorized", 401, Collections.singletonList("WWW-Authenticate"));
  }

  @Test
  void shouldGenerateApiErrorForJaxRsNotSupportedException() {
    testJaxRsException("NotSupported", 415, emptyList());
  }

  @Test
  void shouldGenerateApiErrorForJaxRsServiceUnavailableException() {
    testJaxRsException("ServiceUnavailable", 503, Collections.singletonList("Retry-After"));
  }

  @Test
  void shouldGenerateApiErrorForJaxRsInternalServerErrorException() {
    testJaxRsException("InternalServerError", 500, emptyList());
  }

  private void testJaxRsException(String exceptionType, int expectedError, List<String> header) {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("jaxrsexception")
            .queryParam("type", exceptionType)
            .request(MediaType.APPLICATION_JSON)
            .get();

    ApiError error = response.readEntity(ApiError.class);
    assertThat(header.stream().allMatch(h -> response.getHeaders().containsKey(h))).isTrue();
    assertThat(error.getTitle()).isNotEmpty();
    assertThat(response.getStatus()).isEqualTo(expectedError);
  }

  @Test
  void shouldUseJsonPropertyNames() {
    HashMap<String, Object> message = new HashMap<>();
    message.put("name", "last");
    HashMap<String, Object> nested = new HashMap<>();
    HashMap<String, Object> nestedNested = new HashMap<>();
    nested.put("myNestedResource", nestedNested);
    message.put("myNestedResource", nested);

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(message));

    ApiError error = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

    assertThat(error.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .containsExactlyInAnyOrder(
            tuple("myNestedResource.myNestedField", "NOT_EMPTY"),
            tuple("myNestedResource.myNestedResource.anotherNestedField", "NOT_EMPTY"));
  }

  @Test
  void shouldSupportCustomValidators() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("Maximilian");
    validationResource.setGender("f");
    validationResource.setLastName("Mustermann");
    validationResource.setSalutation("herr");

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

    ApiError error = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
    assertThat(error.getInvalidParams())
        .extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode)
        .containsExactlyInAnyOrder(
            new Tuple("salutation", "All letters must be UPPER CASE only.", "UPPER_CASE"));
  }

  @Test
  void shouldGetSeveralValidationInformation() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName(null);
    validationResource.setGender("z");
    validationResource.setLastName("asb");

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(apiError.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
    assertThat(apiError.getInvalidParams().size()).isEqualTo(3);
    assertThat(apiError.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .containsExactlyInAnyOrder(
            new Tuple("name", "NOT_EMPTY"),
            new Tuple("gender", "ONE_OF"),
            new Tuple("lastName", "PATTERN"));
  }

  @Test
  void shouldGetCustomValidationException() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("Asb");
    validationResource.setLastName("Asb");

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(apiError.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
    assertThat(apiError.getInvalidParams().size()).isEqualTo(2);
    assertThat(apiError.getInvalidParams())
        .extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode)
        .containsExactlyInAnyOrder(
            new Tuple("name", "First name and last name must be different.", "CHECK_NAME_REPEATED"),
            new Tuple(
                "lastName", "First name and last name must be different.", "CHECK_NAME_REPEATED"));
  }

  @Test
  void shouldGetValidationExceptionWithInvalidFieldOfExtendingResource() {
    NameSearchFilterResource emptyNameSearchFilterResource = new NameSearchFilterResource();
    emptyNameSearchFilterResource.setNestedResource(
        new NestedResource().setAnotherNestedResource(new NestedNestedResource()));

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("searchValidation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(emptyNameSearchFilterResource));

    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(apiError.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
    assertThat(apiError.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .containsExactlyInAnyOrder(
            tuple("name", "NOT_NULL"),
            tuple("nestedResource.myNestedField", "NOT_EMPTY"),
            tuple("nestedResource.myNestedResource.anotherNestedField", "NOT_EMPTY"));
  }

  @Test
  void shouldReturnDetailsForValidationMethods() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("noname");
    validationResource.setGender("f");
    validationResource.setLastName("Test");

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(apiError.getInvalidParams().size()).isEqualTo(1);
  }

  @Test
  void shouldMapValidationException() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("validationException");
    validationResource.setGender("f");
    validationResource.setLastName("Test");

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(apiError.getTitle()).isEqualTo("Failed to validate message.");
  }

  @Test
  void shouldReturnApiErrorWhenJsonNotReadable() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("Nothing"));

    assertThat(response.getStatus()).isEqualTo(400);
    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(apiError.getTitle()).startsWith("Failed to process json:");
  }

  @Test
  void shouldReturnApiErrorWhenJsonNotProcessable() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(
                Entity.json(
                    "{\"gender\": \"m\", \"firstName\": \"Hans\", \n"
                        + "\"myNestedResource\": { \"myNestedField\": \"test\", \"someNumber\": \"twenty\" } }"));

    assertThat(response.getStatus()).isEqualTo(400);
    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(apiError.getTitle())
        .isEqualTo(
            "Failed to process json: "
                + "Location 'line: 2, column: 62'; FieldName 'myNestedResource.someNumber'");
  }

  // Jackson tests
  @Test
  void shouldGetJohnDoe() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe") // NOSONAR
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe",
            "John",
            "Doe",
            "Johnny"); // NOSONAR
  }

  @Test
  void shouldRenderEmbeddedResource() {
    Map<String, Object> johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "nickName") // NOSONAR
            .queryParam("embed", "address")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<Map<String, Object>>() {});

    assertThat(johnny)
        .containsKeys("_links", "nickName")
        .doesNotContainKeys("firstName", "lastName"); // NOSONAR
    assertThat(johnny)
        .extracting("_embedded")
        .extracting("address")
        .asList()
        .extracting("city")
        .containsExactly("Hamburg");
  }

  @Test
  void shouldNotRenderOmittedFields() {
    Map<String, Object> johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<Map<String, Object>>() {});

    assertThat(johnny)
        .containsKeys("_links", "nickName")
        .doesNotContainKeys("firstName", "lastName");
  }

  @Test
  void shouldFilterField() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", null, null, "Johnny");
  }

  @Test
  void shouldFilterFieldsByMultipleParams() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "firstName")
            .queryParam("fields", "lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", null);
  }

  @Test
  void shouldFilterFieldsBySingleParams() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "firstName, lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", null);
  }

  @Test
  void shouldFilterNickName() {
    PersonWithChildrenResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe-and-children")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonWithChildrenResource.class);

    assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonWithChildrenResource::getFirstName,
            PersonWithChildrenResource::getLastName,
            PersonWithChildrenResource::getNickName,
            PersonWithChildrenResource::getChildren)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", null, null, "Johnny", null);
  }

  @Test
  void shouldFilterNickNameInList() {
    List<PersonWithChildrenResource> people =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<PersonWithChildrenResource>>() {});

    assertThat(people)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonWithChildrenResource::getFirstName,
            PersonWithChildrenResource::getLastName,
            PersonWithChildrenResource::getNickName,
            PersonWithChildrenResource::getChildren)
        .containsExactly(
            tuple(
                "http://localhost:" + DW.getLocalPort() + "/people/jdoe",
                null,
                null,
                "Johnny",
                null));
  }

  @Test
  void shouldFilterNotInNestedList() {
    List<PersonWithChildrenResource> people =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .queryParam("fields", "nickName,children")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<PersonWithChildrenResource>>() {});

    assertThat(people)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonWithChildrenResource::getFirstName,
            PersonWithChildrenResource::getLastName,
            PersonWithChildrenResource::getNickName)
        .containsExactly(
            tuple("http://localhost:" + DW.getLocalPort() + "/people/jdoe", null, null, "Johnny"));
    assertThat(people.get(0).getChildren())
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            tuple("http://localhost:" + DW.getLocalPort() + "/ydoe", "Yasmine", "Doe", "Yassie"));
  }

  @Test
  void shouldFilterNickNameButNotInNestedList() {
    PersonWithChildrenResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe-and-children")
            .queryParam("fields", "nickName,children")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonWithChildrenResource.class);

    assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonWithChildrenResource::getFirstName,
            PersonWithChildrenResource::getLastName,
            PersonWithChildrenResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", null, null, "Johnny");
    assertThat(johnny.getChildren())
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            tuple("http://localhost:" + DW.getLocalPort() + "/ydoe", "Yasmine", "Doe", "Yassie"));
  }

  @Test
  void shouldGetErrorForRuntimeException() {
    try {
      DW.client()
          .target("http://localhost:" + DW.getLocalPort())
          .path("/runtimeException")
          .request(MediaType.APPLICATION_JSON)
          .get(PersonResource.class);
    } catch (WebApplicationException e) {
      Response response = e.getResponse();
      assertThat(response.getStatus()).isEqualTo(500);
      ApiError details = response.readEntity(ApiError.class);
      assertThat(details.getTitle()).isNotBlank().doesNotContain("RuntimeException");
    }
  }

  @Test
  void shouldGetQueryParameter() {
    String q =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("requiredQuery")
            .queryParam("q", "My Value")
            .request(MediaType.APPLICATION_JSON)
            .get(String.class);

    assertThat(q).isEqualTo("My Value");
  }

  @Test
  void shouldGetErrorForMissingQueryParameter() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("requiredQuery")
            .request(MediaType.APPLICATION_JSON)
            .get();

    ApiError error = response.readEntity(ApiError.class);
    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

    assertThat(error.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .containsExactly(tuple("q", "NOT_EMPTY"));
  }
}
