package org.sdase.commons.server.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.security.test.ForwardedHeaderTestApp;

class ForwardedPrefixFilterTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          ForwardedHeaderTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-with-forwarded-header.yaml"));

  @Test
  void useForwardedPrefixHeader() {

    final String forwardedHost = "forwarded-host";
    final String forwardedProtocol = "https";
    final String forwardedPrefix = "path-segment";

    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort() + "/api")
            .path(ForwardedHeaderTestApp.RESOURCE_PATH)
            .request()
            .header("X-Forwarded-Host", forwardedHost)
            .header("X-Forwarded-Proto", forwardedProtocol)
            .header("X-Forwarded-Prefix", forwardedPrefix)
            .post(Entity.json(null))) {

      assertThat(response.getHeaders().get("location"))
          .containsExactly("https://forwarded-host/path-segment/api/test-resources/123");
    }
  }
}
