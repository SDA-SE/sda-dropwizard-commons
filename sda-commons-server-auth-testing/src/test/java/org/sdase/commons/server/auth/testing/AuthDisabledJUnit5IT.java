package org.sdase.commons.server.auth.testing;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;

class AuthDisabledJUnit5IT {

  @Order(0)
  @RegisterExtension
  static AuthClassExtension AUTH = AuthClassExtension.builder().withDisabledAuth().build();

  @Order(1)
  @RegisterExtension
  static final DropwizardAppExtension<AuthTestConfig> DW =
      new DropwizardAppExtension<>(
          AuthTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  void shouldAccessOpenEndPointWithoutToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/open")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    assertThat(response.readEntity(String.class)).isEqualTo("We are open.");
  }

  @Test
  void shouldAccessSecureEndPointWithoutToken() {
    Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("/secure")
            .request(APPLICATION_JSON)
            .get();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
  }

  @Test
  void shouldThrowExceptionIfRequestingTokenWhileAuthIsDisabled() {
    assertThatCode(AUTH::auth).isInstanceOf(IllegalStateException.class);
  }
}
