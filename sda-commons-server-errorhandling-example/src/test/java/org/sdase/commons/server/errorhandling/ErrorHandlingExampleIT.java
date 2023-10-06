package org.sdase.commons.server.errorhandling;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

class ErrorHandlingExampleIT {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(ErrorHandlingExampleApplication.class, null, randomPorts());

  @Test
  void shouldGetNotFoundException() {
    Response response = getClient().path("exception").request(MediaType.APPLICATION_JSON).get();

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.readEntity(ApiError.class).getTitle())
        .isEqualTo("Not Found: HTTP 404 Not Found");
  }

  @Test
  void shouldGetErrorResponse() {
    Response response = getClient().path("errorResponse").request(MediaType.APPLICATION_JSON).get();

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.readEntity(ApiError.class).getTitle())
        .isEqualTo("ApiError thrown in code to be used in response");
  }

  @Test
  void shouldGetErrorResponseFromApiException() {
    Response response = getClient().path("apiException").request(MediaType.APPLICATION_JSON).get();

    assertThat(response.getStatus()).isEqualTo(422);
    assertThat(response.readEntity(ApiError.class).getTitle()).isEqualTo("Semantic exception");
  }

  @Test
  void shouldGetValidationException() {
    Response response =
        getClient()
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("{ \"param1\": \"\" }"));

    assertThat(response.getStatus()).isEqualTo(422);
    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(apiError.getTitle()).isEqualTo("Request parameters are not valid.");
    assertThat(apiError.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .contains(new Tuple("param1", "NOT_EMPTY"));
  }

  @Test
  void shouldGetCustomValidationException() {
    Response response =
        getClient()
            .path("validation")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json("{ \"param1\": \"lowercase\" }"));

    assertThat(response.getStatus()).isEqualTo(422);
    ApiError apiError = response.readEntity(ApiError.class);
    assertThat(apiError.getTitle()).isEqualTo("Request parameters are not valid.");
    assertThat(apiError.getInvalidParams())
        .extracting(ApiInvalidParam::getField, ApiInvalidParam::getErrorCode)
        .contains(new Tuple("param1", "UPPER_CASE"));
  }

  private WebTarget getClient() {
    return DW.client().target("http://localhost:" + DW.getLocalPort()).path("errors");
  }
}
