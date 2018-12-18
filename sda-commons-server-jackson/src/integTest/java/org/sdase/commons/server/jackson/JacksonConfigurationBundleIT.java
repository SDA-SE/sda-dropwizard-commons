package org.sdase.commons.server.jackson;


import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.assertj.core.groups.Tuple;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.jackson.test.JacksonConfigurationTestApp;
import org.sdase.commons.server.jackson.test.PersonResource;
import org.sdase.commons.server.jackson.test.PersonWithChildrenResource;
import org.sdase.commons.server.jackson.test.ValidationResource;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class JacksonConfigurationBundleIT {

   private final static String VALIDATION_ERROR_MESSAGE = "Request parameters are not valid.";

   @ClassRule
   public static final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(JacksonConfigurationTestApp.class,
         ResourceHelpers.resourceFilePath("test-config.yaml"));

   // Validation and Error Tests
   @Test
   public void shouldGetError() {
      try {
         DW
               .client()
               .target("http://localhost:" + DW.getLocalPort())
               .path("/exception")
               .request(MediaType.APPLICATION_JSON)
               .get(PersonResource.class);
      } catch (WebApplicationException e) {
         ApiError details = e.getResponse().readEntity(ApiError.class);
         assertThat(details)
               .extracting(ApiError::getTitle, d -> d.getInvalidParams().get(0).getField(),
                     d -> d.getInvalidParams().get(0).getReason(), d -> d.getInvalidParams().get(0).getErrorCode())
               .containsExactly("Some exception", "parameter", null, "SOME_ERROR_CODE");
      }
   }

   // Validation and Error Tests
   @Test
   public void shouldGenerateApiErrorForJaxRsExceptions() {
      testJaxRsException("NotFound", 404, emptyList());
      testJaxRsException("BadRequest", 400, emptyList());
      testJaxRsException("Forbidden", 403, emptyList());
      testJaxRsException("NotAcceptable", 406, emptyList());
      testJaxRsException("NotAllowed", 405, Collections.singletonList("Allow"));
      testJaxRsException("NotAuthorized", 401, Collections.singletonList("WWW-Authenticate"));
      testJaxRsException("NotSupported", 415, emptyList());
      testJaxRsException("ServiceUnavailable", 503, Collections.singletonList("Retry-After"));
      testJaxRsException("InternalServerError", 500, emptyList());
   }

   private void testJaxRsException(String exceptionType, int expectedError, List<String> header) {
      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/jaxrsexception")
            .queryParam("type", exceptionType)
            .request(MediaType.APPLICATION_JSON)
            .get();

      ApiError error = response.readEntity(ApiError.class);
      assertThat(header.stream().allMatch(h -> response.getHeaders().containsKey(h))).isTrue();
      assertThat(error.getTitle()).isNotEmpty();
      assertThat(response.getStatus()).isEqualTo(expectedError);
   }

   @Test
   public void shouldUseJsonPropertyNames() {
      HashMap<String, Object> message = new HashMap<>();
      message.put("name", "last");
      HashMap<String, Object> nested = new HashMap<>();
      message.put("myNestedResource", nested);

      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(message));

      ApiError error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);

      assertThat(error.getInvalidParams().get(0))
            .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
            .containsExactly("myNestedResource.myNestedField", "NOT_EMPTY");
   }


   @Test
   public void shouldSupportCustomValidators() {
      ValidationResource validationResource = new ValidationResource();
      validationResource.setFirstName("Maximilian");
      validationResource.setGender("f");
      validationResource.setLastName("Mustermann");
      validationResource.setSalutation("herr");

      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

      ApiError error = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(error.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
      assertThat(error.getInvalidParams()).extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode
      ).containsExactlyInAnyOrder(
            new Tuple("salutation", "All letters must be UPPER CASE only.", "UPPER_CASE")
      );
   }

   @Test
   public void shouldGetSeveralValidationInformation() {
      ValidationResource validationResource = new ValidationResource();
      validationResource.setFirstName(null);
      validationResource.setGender("z");
      validationResource.setLastName("asb");

      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(apiError.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
      assertThat(apiError.getInvalidParams().size()).isEqualTo(3);
      assertThat(apiError.getInvalidParams()).extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getErrorCode
      ).containsExactlyInAnyOrder(
            new Tuple("name", "NOT_EMPTY"),
            new Tuple("gender", "ONE_OF"),
            new Tuple("lastName", "PATTERN")
      );
   }

   @Test
   public void shouldGetCustomValidationException() {
      ValidationResource validationResource = new ValidationResource();
      validationResource.setFirstName("Asb");
      validationResource.setLastName("Asb");

      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));

      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(apiError.getTitle()).isEqualTo(VALIDATION_ERROR_MESSAGE);
      assertThat(apiError.getInvalidParams().size()).isEqualTo(2);
      assertThat(apiError.getInvalidParams()).extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getReason, ApiInvalidParam::getErrorCode
      ).containsExactlyInAnyOrder(
            new Tuple("name", "First name and last name must be different.", "CHECK_NAME_REPEATED"),
            new Tuple("lastName", "First name and last name must be different.", "CHECK_NAME_REPEATED")
      );
   }


   @Test
   public void shouldReturnDetailsForValidationMethods() {
      ValidationResource validationResource = new ValidationResource();
      validationResource.setFirstName("noname");
      validationResource.setGender("f");
      validationResource.setLastName("Test");

      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(validationResource));


      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(response.getStatus()).isEqualTo(422);
      assertThat(apiError.getInvalidParams().size()).isEqualTo(1);
   }

    @Test
    public void shouldMapValidationException() {
       ValidationResource validationResource = new ValidationResource();
       validationResource.setFirstName("validationException");
       validationResource.setGender("f");
       validationResource.setLastName("Test");

       Response response = DW
             .client()
             .target("http://localhost:" + DW.getLocalPort())
             .path("/validation")
             .request(MediaType.APPLICATION_JSON)
             .post(Entity.json(validationResource));

       ApiError apiError = response.readEntity(ApiError.class);
       assertThat(response.getStatus()).isEqualTo(500);
       assertThat(apiError.getTitle()).isEqualTo("Failed to validate message.");
    }


   @Test
   public void shouldReturnApiErrorWhenJsonNotReadable() {
      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("Nothing"));

      assertThat(response.getStatus()).isEqualTo(400);
      ApiError apiError = response.readEntity(ApiError.class);
      assertThat(apiError.getTitle()).startsWith("Failed to parse json.");
   }

   // Jackson tests
   @Test
   public void shouldGetJohnDoe() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people").path("jdoe")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", "Johnny");
   }

   @Test
   public void shouldNotRenderOmittedFields() {
      Map<String, Object> johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people").path("jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<Map<String, Object>>() {
            });

      assertThat(johnny).containsKeys("_links", "nickName").doesNotContainKeys("firstName", "lastName");
   }

   @Test
   public void shouldFilterField() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people").path("jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/people/jdoe", null, null, "Johnny");
   }

   @Test
   public void shouldFilterFieldsByMultipleParams() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people").path("jdoe")
            .queryParam("fields", "firstName")
            .queryParam("fields", "lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", null);
   }

   @Test
   public void shouldFilterFieldsBySingleParams() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people").path("jdoe")
            .queryParam("fields", "firstName, lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", null);
   }

   @Test
   public void shouldFilterNickName() {
      PersonWithChildrenResource johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("people").path("jdoe-and-children")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonWithChildrenResource.class);

      assertThat(johnny)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonWithChildrenResource::getFirstName,
                  PersonWithChildrenResource::getLastName,
                  PersonWithChildrenResource::getNickName,
                  PersonWithChildrenResource::getChildren
            )
            .containsExactly(
                  "http://localhost:" + DW.getLocalPort() + "/people/jdoe",
                  null,
                  null,
                  "Johnny",
                  null
            );
   }

   @Test
   public void shouldFilterNickNameInList() {
      List<PersonWithChildrenResource> people = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("people")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<PersonWithChildrenResource>>() {});

      assertThat(people)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonWithChildrenResource::getFirstName,
                  PersonWithChildrenResource::getLastName,
                  PersonWithChildrenResource::getNickName,
                  PersonWithChildrenResource::getChildren
            )
            .containsExactly(
                  tuple(
                        "http://localhost:" + DW.getLocalPort() + "/people/jdoe",
                        null,
                        null,
                        "Johnny",
                        null
                  )
            );
   }

   @Test
   public void shouldFilterNotInNestedList() {
      List<PersonWithChildrenResource> people = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("people")
            .queryParam("fields", "nickName,children")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<PersonWithChildrenResource>>() {});

      assertThat(people)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonWithChildrenResource::getFirstName,
                  PersonWithChildrenResource::getLastName,
                  PersonWithChildrenResource::getNickName
            )
            .containsExactly(
                  tuple(
                        "http://localhost:" + DW.getLocalPort() + "/people/jdoe",
                        null,
                        null,
                        "Johnny"
                  )
            );
      assertThat(people.get(0).getChildren())
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonResource::getFirstName,
                  PersonResource::getLastName,
                  PersonResource::getNickName
            )
            .containsExactly(
                  tuple("http://localhost:" + DW.getLocalPort() + "/ydoe", "Yasmine", "Doe", "Yassie")
            );

   }

   @Test
   public void shouldFilterNickNameButNotInNestedList() {
      PersonWithChildrenResource johnny = DW.client()
            .target("http://localhost:" + DW.getLocalPort()).path("people").path("jdoe-and-children")
            .queryParam("fields", "nickName,children")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonWithChildrenResource.class);

      assertThat(johnny)
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonWithChildrenResource::getFirstName,
                  PersonWithChildrenResource::getLastName,
                  PersonWithChildrenResource::getNickName
            )
            .containsExactly(
                  "http://localhost:" + DW.getLocalPort() + "/people/jdoe",
                  null,
                  null,
                  "Johnny"
            );
      assertThat(johnny.getChildren())
            .extracting(
                  p -> p.getSelf().getHref(),
                  PersonResource::getFirstName,
                  PersonResource::getLastName,
                  PersonResource::getNickName
            )
            .containsExactly(
                  tuple("http://localhost:" + DW.getLocalPort() + "/ydoe", "Yasmine", "Doe", "Yassie")
            );
   }

   @Test
   public void shouldGetErrorForRuntimeException() {
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

}
