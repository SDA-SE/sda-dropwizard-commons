package org.sdase.commons.server.jackson;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dropwizard.core.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.jackson.test.JacksonConfigurationTestApp;
import org.sdase.commons.server.jackson.test.NameSearchFilterResource;
import org.sdase.commons.server.jackson.test.NestedNestedResource;
import org.sdase.commons.server.jackson.test.NestedResource;
import org.sdase.commons.server.jackson.test.PersonResource;
import org.sdase.commons.server.jackson.test.PersonSearchResultResource;
import org.sdase.commons.server.jackson.test.PersonWithChildrenResource;
import org.sdase.commons.server.jackson.test.ValidationResource;
import org.sdase.commons.server.testing.DropwizardLegacyHelper;
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
      DropwizardLegacyHelper.client(DW.getObjectMapper())
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
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("jaxrsexception")
            .queryParam("type", exceptionType)
            .request(MediaType.APPLICATION_JSON)
            .get()) {

      ApiError error = response.readEntity(ApiError.class);
      assertThat(header.stream().allMatch(h -> response.getHeaders().containsKey(h))).isTrue();
      assertThat(error.getTitle()).isNotEmpty();
      assertThat(response.getStatus()).isEqualTo(expectedError);
    }
  }

  @Test
  void shouldUseJsonPropertyNames() {
    HashMap<String, Object> message = new HashMap<>();
    message.put("name", "last");
    HashMap<String, Object> nested = new HashMap<>();
    HashMap<String, Object> nestedNested = new HashMap<>();
    nested.put("myNestedResource", nestedNested);
    message.put("myNestedResource", nested);

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(message))) {

      ApiError error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

      assertThat(error.getInvalidParams())
          .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
          .containsExactlyInAnyOrder(
              tuple("myNestedResource.myNestedField", "NOT_EMPTY"),
              tuple("myNestedResource.myNestedResource.anotherNestedField", "NOT_EMPTY"));
    }
  }

  @Test
  void shouldSupportCustomValidators() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("Maximilian");
    validationResource.setGender("f");
    validationResource.setLastName("Mustermann");
    validationResource.setSalutation("herr");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource))) {

      ApiError error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
      assertThat(error.getInvalidParams())
          .extracting(
              ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode)
          .containsExactlyInAnyOrder(
              new Tuple("salutation", "All letters must be UPPER CASE only.", "UPPER_CASE"));
    }
  }

  @Test
  void shouldGetSeveralValidationInformation() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName(null);
    validationResource.setGender("z");
    validationResource.setLastName("asb");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource))) {

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
  }

  @Test
  void shouldGetCustomValidationException() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("Asb");
    validationResource.setLastName("Asb");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource))) {

      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(apiError.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
      assertThat(apiError.getInvalidParams().size()).isEqualTo(2);
      assertThat(apiError.getInvalidParams())
          .extracting(
              ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode)
          .containsExactlyInAnyOrder(
              new Tuple(
                  "name", "First name and last name must be different.", "CHECK_NAME_REPEATED"),
              new Tuple(
                  "lastName",
                  "First name and last name must be different.",
                  "CHECK_NAME_REPEATED"));
    }
  }

  @Test
  void shouldGetValidationExceptionWithInvalidFieldOfExtendingResource() {
    NameSearchFilterResource emptyNameSearchFilterResource = new NameSearchFilterResource();
    emptyNameSearchFilterResource.setNestedResource(
        new NestedResource().setAnotherNestedResource(new NestedNestedResource()));

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("searchValidation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(emptyNameSearchFilterResource))) {

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
  }

  @Test
  void shouldReturnDetailsForValidationMethods() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("noname");
    validationResource.setGender("f");
    validationResource.setLastName("Test");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource))) {

      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(apiError.getInvalidParams().size()).isEqualTo(1);
    }
  }

  @Test
  void shouldMapValidationException() {
    ValidationResource validationResource = new ValidationResource();
    validationResource.setFirstName("validationException");
    validationResource.setGender("f");
    validationResource.setLastName("Test");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource))) {

      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(apiError.getTitle()).isEqualTo("Failed to validate message.");
    }
  }

  @Test
  void shouldReturnApiErrorWhenJsonNotReadable() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("Nothing"))) {

      assertThat(response.getStatus()).isEqualTo(400);
      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(apiError.getTitle()).startsWith("Failed to process json:");
    }
  }

  @Test
  void shouldReturnApiErrorWhenJsonNotProcessable() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(
                Entity.json(
                    "{\"gender\": \"m\", \"firstName\": \"Hans\", \n"
                        + "\"myNestedResource\": { \"myNestedField\": \"test\", \"someNumber\": \"twenty\" } }"))) {

      assertThat(response.getStatus()).isEqualTo(400);
      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(apiError.getTitle())
          .isEqualTo(
              "Failed to process json: "
                  + "Location 'line: 2, column: 62'; FieldName 'myNestedResource.someNumber'");
    }
  }

  // Jackson tests
  @Test
  void shouldGetJohnDoe() {
    PersonResource johnny =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
  void shouldFilterChildrenAndNestedFieldsByPathIfFlagIsTrue() {
    JsonNode johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe-and-children-with-flag")
            .queryParam(
                "fields",
                "children.nickName,renamedCustomProp.myNestedResource.anotherNestedField,address.city")
            .request(MediaType.APPLICATION_JSON)
            .get(JsonNode.class);

    assertThat(johnny.has("_links")).isTrue();
    assertThat(johnny.has("children")).isTrue();
    assertThat(johnny.has("renamedCustomProp")).isTrue();
    assertThat(johnny.has("address")).isTrue();

    assertThat(johnny.has("firstName")).isFalse();
    assertThat(johnny.has("lastName")).isFalse();
    assertThat(johnny.has("nickName")).isFalse();

    JsonNode child = johnny.path("children").get(0);
    assertThat(child.path("nickName").asText()).isEqualTo("Yassie");
    assertThat(child.has("_links")).isTrue();
    assertThat(child.has("firstName")).isFalse();
    assertThat(child.has("lastName")).isFalse();

    JsonNode customNested = johnny.path("renamedCustomProp");
    assertThat(customNested.size()).isEqualTo(1);
    assertThat(customNested.has("myNestedResource")).isTrue();

    JsonNode nestedResource = customNested.path("myNestedResource");
    assertThat(nestedResource.size()).isEqualTo(1);
    assertThat(nestedResource.path("anotherNestedField").asText()).isEqualTo("deep");
    assertThat(nestedResource.has("someNumber")).isFalse();

    assertThat(johnny.path("address").size()).isEqualTo(1);
    assertThat(johnny.path("address").path("city").asText()).isEqualTo("Hamburg");
  }

  @Test
  void shouldMatchNestedChildrenDocumentationExample() {
    JsonNode response =
        readFieldFilterFragment("nested-children.http", "nested-children.json", "children");

    assertThat(response.path("children").get(0).path("nickName").asText()).isEqualTo("Yassie");
  }

  @Test
  void shouldKeepFullSubtreeForNestedAnnotatedChildByDefault() {
    JsonNode johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe-and-children")
            .queryParam(
                "fields",
                "children.nickName,renamedCustomProp.myNestedResource.anotherNestedField,address.city")
            .request(MediaType.APPLICATION_JSON)
            .get(JsonNode.class);

    JsonNode child = johnny.path("children").get(0);
    assertThat(child.has("firstName")).isTrue();
    assertThat(child.has("lastName")).isTrue();

    JsonNode customNested = johnny.path("renamedCustomProp");
    assertThat(customNested.has("myNestedField")).isTrue();
    assertThat(customNested.has("someNumber")).isTrue();

    JsonNode nestedResource = customNested.path("myNestedResource");
    assertThat(nestedResource.has("anotherNestedField")).isTrue();
    assertThat(nestedResource.has("someNumber")).isTrue();

    assertThat(johnny.path("address").has("id")).isTrue();
    assertThat(johnny.path("address").has("city")).isTrue();
  }

  @Test
  void shouldKeepFullSubtreeForParentPathsFromRepeatedFieldParams() {
    JsonNode johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe-and-children")
            .queryParam("fields", "children")
            .queryParam("fields", "renamedCustomProp")
            .request(MediaType.APPLICATION_JSON)
            .get(JsonNode.class);

    assertThat(johnny.has("_links")).isTrue();
    assertThat(johnny.has("children")).isTrue();
    assertThat(johnny.has("renamedCustomProp")).isTrue();
    assertThat(johnny.has("firstName")).isFalse();
    assertThat(johnny.has("lastName")).isFalse();
    assertThat(johnny.has("nickName")).isFalse();
    assertThat(johnny.has("address")).isFalse();

    JsonNode child = johnny.path("children").get(0);
    assertThat(child.has("_links")).isTrue();
    assertThat(child.path("firstName").asText()).isEqualTo("Yasmine");
    assertThat(child.path("lastName").asText()).isEqualTo("Doe");
    assertThat(child.path("nickName").asText()).isEqualTo("Yassie");

    JsonNode customNested = johnny.path("renamedCustomProp");
    assertThat(customNested.has("myNestedField")).isTrue();
    assertThat(customNested.has("someNumber")).isTrue();
    assertThat(customNested.has("myNestedResource")).isTrue();

    JsonNode nestedResource = customNested.path("myNestedResource");
    assertThat(nestedResource.has("anotherNestedField")).isTrue();
    assertThat(nestedResource.has("someNumber")).isTrue();
  }

  @Test
  void shouldKeepFullSubtreeForUnannotatedChildEvenIfSubFieldIsRequested() {
    JsonNode johnny =
        readFieldFilterFragment(
            "unfiltered-child.http", "unfiltered-child.json", "unfilteredChild");

    JsonNode unfilteredChild = johnny.path("unfilteredChild");
    assertThat(unfilteredChild.path("name").asText()).isEqualTo("Jane");
    assertThat(unfilteredChild.path("lastName").asText()).isEqualTo("Doey");
  }

  @Test
  void shouldKeepFullSubtreeForUnannotatedListChildEvenIfSubFieldIsRequested() {
    JsonNode response =
        readFieldFilterFragment(
            "unfiltered-list-child.http", "unfiltered-list-child.json", "unfilteredChildren");

    JsonNode unfilteredChild = response.path("unfilteredChildren").get(0);
    assertThat(unfilteredChild.path("name").asText()).isEqualTo("Janet");
    assertThat(unfilteredChild.path("lastName").asText()).isEqualTo("Doe");
  }

  @Test
  void shouldMatchParentListDocumentationExample() {
    JsonNode response =
        readFieldFilterFragment("parent-children.http", "parent-children.json", "children");

    assertThat(response.path("children").get(0).path("firstName").asText()).isEqualTo("Yasmine");
    assertThat(response.path("children").get(0).path("lastName").asText()).isEqualTo("Doe");
    assertThat(response.path("children").get(0).path("nickName").asText()).isEqualTo("Yassie");
  }

  @Test
  void shouldFilterNestedFieldsInsideMapValuesWithoutTreatingKeysAsPathSegments() {
    JsonNode response = readFieldFilterExample("nested-fields.http", "nested-fields.json");

    assertThat(response.has("attributes")).isTrue();
    assertThat(response.has("id")).isFalse();

    JsonNode attributes = response.path("attributes");
    assertThat(attributes.path("alpha").path("name").asText()).isEqualTo("first");
    assertThat(attributes.path("alpha").has("description")).isFalse();
    assertThat(attributes.path("beta").path("name").asText()).isEqualTo("second");
    assertThat(attributes.path("beta").has("description")).isFalse();
  }

  @Test
  void shouldKeepFullMapWhenParentFieldRequested() {
    JsonNode response = readFieldFilterExample("full-subtree.http", "full-subtree.json");

    assertThat(response.has("attributes")).isTrue();
    assertThat(response.has("id")).isFalse();

    JsonNode attributes = response.path("attributes");
    assertThat(attributes.path("alpha").path("name").asText()).isEqualTo("first");
    assertThat(attributes.path("alpha").has("description")).isTrue();
    assertThat(attributes.path("beta").path("name").asText()).isEqualTo("second");
    assertThat(attributes.path("beta").has("description")).isTrue();
  }

  @Test
  void shouldKeepFullMapSubtreeWhenNestedFilteringDisabled() {
    JsonNode response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("attributes-without-flag")
            .queryParam("fields", "attributes.name")
            .request(MediaType.APPLICATION_JSON)
            .get(JsonNode.class);

    assertThat(response.has("attributes")).isTrue();
    assertThat(response.has("id")).isFalse();

    JsonNode attributes = response.path("attributes");
    assertThat(attributes.path("alpha").path("name").asText()).isEqualTo("first");
    assertThat(attributes.path("alpha").path("description").asText()).isEqualTo("hidden-one");
    assertThat(attributes.path("beta").path("name").asText()).isEqualTo("second");
    assertThat(attributes.path("beta").path("description").asText()).isEqualTo("hidden-two");
  }

  @Test
  void shouldKeepExcludedNestedFieldAsNullInDefaultMode() {
    PersonSearchResultResource result =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("search-result-list")
            .queryParam("fields", "results.name")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonSearchResultResource.class);

    assertThat(result.getResults()).hasSize(1);
    assertThat(result.getResults().get(0).getName()).isEqualTo("bundle-a");
    assertThat(result.getResults().get(0).getCustomMap()).isNull();
  }

  @Test
  void shouldMatchWrappedListDocumentationExample() {
    JsonNode response = readFieldFilterExample("wrapped-results.http", "wrapped-results.json");

    assertThat(response.path("results").get(0).path("name").asText()).isEqualTo("bundle-a");
    assertThat(response.path("results").get(0).path("customMap").isNull()).isTrue();
  }

  @Test
  void shouldFilterNickNameInList() {
    List<PersonWithChildrenResource> people =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
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
      DropwizardLegacyHelper.client(DW.getObjectMapper())
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
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("requiredQuery")
            .queryParam("q", "My Value")
            .request(MediaType.APPLICATION_JSON)
            .get(String.class);

    assertThat(q).isEqualTo("My Value");
  }

  @Test
  void shouldGetErrorForMissingQueryParameter() {
    ApiError error;
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("requiredQuery")
            .request(MediaType.APPLICATION_JSON)
            .get()) {

      error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
    }
    assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

    assertThat(error.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .containsExactly(tuple("q", "NOT_EMPTY"));
  }

  @Test
  void shouldAcceptSubtypeOne() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("subtypes")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Collections.singletonMap("type", "ONE")))) {
      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation().getPath()).isEqualTo("/subtypes/ONE");
    }
  }

  @Test
  void shouldAcceptSubtypeTwo() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("subtypes")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Collections.singletonMap("type", "TWO")))) {
      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation().getPath()).isEqualTo("/subtypes/TWO");
    }
  }

  @Test
  void shouldNotAcceptSubtypeUndefinedSubtypeThree() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("subtypes")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Collections.singletonMap("type", "THREE")))) {
      assertThat(response.getStatus()).isEqualTo(422);
    }
  }

  @Test
  void shouldAcceptSubtypeOneFaultTolerant() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("subtypesTolerant")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Collections.singletonMap("type", "ONE")))) {
      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation().getPath()).isEqualTo("/subtypesTolerant/ONE");
    }
  }

  @Test
  void shouldAcceptSubtypeTwoFaultTolerant() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("subtypesTolerant")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Collections.singletonMap("type", "TWO")))) {
      assertThat(response.getStatus()).isEqualTo(201);
      assertThat(response.getLocation().getPath()).isEqualTo("/subtypesTolerant/TWO");
    }
  }

  @Test
  void shouldNotAcceptSubtypeUndefinedSubtypeThreeFaultTolerant() {
    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("subtypesTolerant")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(Collections.singletonMap("type", "THREE")))) {
      assertThat(response.getStatus()).isEqualTo(422);
    }
  }

  @Test
  void shouldGetErrorForInvalidFormParameter() {
    Form form = new Form();
    form.param("foo", "");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .target("http://localhost:" + DW.getLocalPort())
            .path("requiredForm")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE))) {

      ApiError error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

      assertThat(error.getInvalidParams())
          .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
          .containsExactly(tuple("foo", "NOT_BLANK"));
    }
  }

  @Test
  void shouldGetErrorForMissingMultipartFormDataParameter() {
    FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
    formDataMultiPart.field("text", "foo");

    try (Response response =
        DropwizardLegacyHelper.client(DW.getObjectMapper())
            .register(MultiPartFeature.class)
            .target("http://localhost:" + DW.getLocalPort())
            .path("requiredFormData")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE))) {

      ApiError error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

      assertThat(error.getInvalidParams())
          .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
          .containsExactly(tuple("file", "NOT_NULL"));
    }
  }

  private static JsonNode readFieldFilterExample(String requestFile, String responseFile) {
    JsonNode actual = requestFieldFilterExample(requestFile);
    assertThat(actual).isEqualTo(readFieldFilterFixture(responseFile));
    return actual;
  }

  private static JsonNode readFieldFilterFragment(
      String requestFile, String responseFile, String responseField) {
    JsonNode actual = requestFieldFilterExample(requestFile);
    assertThat(removeHalLinks(actual.path(responseField).deepCopy()))
        .isEqualTo(readFieldFilterFixture(responseFile).path(responseField));
    return actual;
  }

  private static JsonNode removeHalLinks(JsonNode node) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      objectNode.remove("_links");
      objectNode.elements().forEachRemaining(JacksonConfigurationBundleIT::removeHalLinks);
    } else if (node.isArray()) {
      node.elements().forEachRemaining(JacksonConfigurationBundleIT::removeHalLinks);
    }
    return node;
  }

  private static JsonNode requestFieldFilterExample(String requestFile) {
    URI request = readFieldFilterRequest(requestFile);
    String query = request.getQuery();
    if (query == null || !query.startsWith("fields=")) {
      throw new IllegalStateException("Expected fields query parameter in " + requestFile);
    }

    JsonNode actual =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path(request.getPath())
            .queryParam("fields", query.substring("fields=".length()))
            .request(MediaType.APPLICATION_JSON)
            .get(JsonNode.class);
    return actual;
  }

  private static URI readFieldFilterRequest(String fileName) {
    try (InputStream resource =
        JacksonConfigurationBundleIT.class.getResourceAsStream("/field-filtering/" + fileName)) {
      if (resource == null) {
        throw new IllegalStateException("Missing field-filter request fixture: " + fileName);
      }
      String request = new String(resource.readAllBytes(), StandardCharsets.UTF_8).trim();
      if (!request.startsWith("GET ")) {
        throw new IllegalStateException("Expected GET request fixture: " + fileName);
      }
      return URI.create(request.substring("GET ".length()).trim());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static JsonNode readFieldFilterFixture(String fileName) {
    try (InputStream resource =
        JacksonConfigurationBundleIT.class.getResourceAsStream("/field-filtering/" + fileName)) {
      if (resource == null) {
        throw new IllegalStateException("Missing field-filter response fixture: " + fileName);
      }
      return DW.getObjectMapper().readTree(resource);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
