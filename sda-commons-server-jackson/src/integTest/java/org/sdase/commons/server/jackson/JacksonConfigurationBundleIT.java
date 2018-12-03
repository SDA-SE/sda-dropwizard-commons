package org.sdase.commons.server.jackson;

import org.assertj.core.groups.Tuple;
import org.sdase.commons.server.jackson.errors.JerseyValidationExceptionMapper;
import org.sdase.commons.server.jackson.test.JacksonConfigurationTestApp;
import org.sdase.commons.server.jackson.test.PersonResource;
import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.jackson.test.ValidationResource;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

public class JacksonConfigurationBundleIT {

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
         Assertions
               .assertThat(details)
               .extracting(ApiError::getTitle, d -> d.getInvalidParams().get(0).getField(),
                     d -> d.getInvalidParams().get(0).getReason(), d -> d.getInvalidParams().get(0).getErrorCode())
               .containsExactly("Some exception", "parameter", null, "MANDATORY_FIELD_IS_MISSING");
      }
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
      Assertions.assertThat(response.getStatus()).isEqualTo(422);
      Assertions.assertThat(error.getTitle()).isEqualTo("Request Parameters are not valid.");

      Assertions
            .assertThat(error.getInvalidParams().get(0))
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
      Assertions.assertThat(response.getStatus()).isEqualTo(422);
      Assertions.assertThat(error.getTitle()).isEqualTo("Request Parameters are not valid.");
      Assertions.assertThat(error.getInvalidParams()).extracting(
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
      Assertions.assertThat(response.getStatus()).isEqualTo(422);
      Assertions.assertThat(apiError.getTitle()).isEqualTo("Request Parameters are not valid.");
      Assertions.assertThat(apiError.getInvalidParams().size()).isEqualTo(3);
      Assertions.assertThat(apiError.getInvalidParams()).extracting(
            ApiInvalidParam::getField, ApiInvalidParam::getErrorCode
      ).containsExactlyInAnyOrder(
            new Tuple("name", "NOT_EMPTY"),
            new Tuple("gender", "ONE_OF"),
            new Tuple("lastName", "PATTERN")
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
      Assertions.assertThat(response.getStatus()).isEqualTo(422);
      Assertions.assertThat(apiError.getInvalidParams().size()).isEqualTo(1);
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
       Assertions.assertThat(response.getStatus()).isEqualTo(500);
       Assertions.assertThat(apiError.getTitle()).isEqualTo("Failed to validate message.");
    }



   @Test
   public void shouldReturnApiErrorWhenJsonNotReadable() {
      Response response = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("Nothing"));

      Assertions.assertThat(response.getStatus()).isEqualTo(400);
      ApiError apiError = response.readEntity(ApiError.class);
      Assertions.assertThat(apiError.getTitle()).startsWith("Failed to parse json.");
   }

   // Jackson tests
   @Test
   public void shouldGetJohnDoe() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/jdoe")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      Assertions
            .assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/jdoe", "John", "Doe", "Johnny");
   }

   @Test
   public void shouldNotRenderOmittedFields() {
      Map<String, Object> johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<Map<String, Object>>() {
            });

      Assertions.assertThat(johnny).containsKeys("_links", "nickName").doesNotContainKeys("firstName", "lastName");
   }

   @Test
   public void shouldFilterField() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      Assertions
            .assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/jdoe", null, null, "Johnny");
   }

   @Test
   public void shouldFilterFieldsByMultipleParams() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/jdoe")
            .queryParam("fields", "firstName")
            .queryParam("fields", "lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      Assertions
            .assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/jdoe", "John", "Doe", null);
   }

   @Test
   public void shouldFilterFieldsBySingleParams() {
      PersonResource johnny = DW
            .client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/jdoe")
            .queryParam("fields", "firstName, lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

      Assertions
            .assertThat(johnny)
            .extracting(p -> p.getSelf().getHref(), PersonResource::getFirstName, PersonResource::getLastName,
                  PersonResource::getNickName)
            .containsExactly("http://localhost:" + DW.getLocalPort() + "/jdoe", "John", "Doe", null);
   }
}
