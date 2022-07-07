package org.sdase.commons.server.jackson;

import static io.dropwizard.testing.ConfigOverride.randomPorts;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.jackson.test.*;
import org.sdase.commons.shared.api.error.ApiError;

class JacksonConfigurationInvalidTypeIT {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          JacksonConfigurationInvalidTypeTestApp.class, null, randomPorts());

  @Test
  void shouldPostResourceWithInheritance() {
    String resource = "{\"type\": \"SubType\"}";

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("resourceWithInheritance")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(resource));

    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
  }

  @Test
  void shouldThrowApiErrorForUnknownSubType() {
    String resource = "{\"type\": \"UnknownSubType\"}";

    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("resourceWithInheritance")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.json(resource));

    Assertions.assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
    ApiError apiError = response.readEntity(ApiError.class);
    Assertions.assertThat(apiError).isNotNull();
    Assertions.assertThat(apiError.getTitle()).isEqualTo("Invalid sub type");
    Assertions.assertThat(apiError.getInvalidParams())
        .extracting("field", "reason", "errorCode")
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                "",
                "Invalid sub type UnknownSubType for base type ResourceWithInheritance",
                "INVALID_SUBTYPE"));
  }
}
