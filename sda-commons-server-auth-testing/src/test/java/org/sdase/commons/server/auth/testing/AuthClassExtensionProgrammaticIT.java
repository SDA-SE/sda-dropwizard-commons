package org.sdase.commons.server.auth.testing;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

class AuthClassExtensionProgrammaticIT {

  @Order(0)
  @RegisterExtension
  static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @Order(1)
  @RegisterExtension
  static final DropwizardAppExtension<AuthTestConfig> DW =
      new DropwizardAppExtension<>(
          AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldAccessOpenEndPointWithoutToken() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/open")
            .request(APPLICATION_JSON)
            .get()) {

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
    }
  }

  @Test
  void shouldNotAccessSecureEndPointWithoutToken() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .get()) {

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }
  }

  @Test
  void shouldAccessSecureEndPointWithToken() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(AUTH.auth().buildAuthHeader())
            .get()) {

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(new GenericType<Map<String, String>>() {}))
          .contains(entry("iss", "AuthExtension"), entry("sub", "test"));
    }
  }

  @Test
  void shouldGetClaimsFromSecureEndPointWithToken() {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .headers(
                AUTH.auth()
                    .addClaim("test", "testClaim")
                    .addClaims(singletonMap("mapKey", "testClaimFromMap"))
                    .buildAuthHeader())
            .get()) {

      assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
      assertThat(response.readEntity(new GenericType<Map<String, String>>() {}))
          .contains(
              entry("iss", "AuthExtension"),
              entry("sub", "test"),
              entry("test", "testClaim"),
              entry("mapKey", "testClaimFromMap"));
    }
  }
}
