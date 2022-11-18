package org.sdase.commons.server.security.filter;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sdase.commons.server.security.test.SecurityTestApp;

class WebSecurityApiOnlyHeaderFilterITest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          SecurityTestApp.class, ResourceHelpers.resourceFilePath("test-config-no-settings.yaml"));

  static Stream<Arguments> data() {
    return Stream.of(
        of("X-Frame-Options", "DENY"),
        of("X-Content-Type-Options", "nosniff"),
        of("X-XSS-Protection", "1; mode=block"),
        of("Referrer-Policy", "same-origin"),
        of("X-Permitted-Cross-Domain-Policies", "none"),
        of(
            "Content-Security-Policy",
            String.join(
                "; ", // NOSONAR
                asList("default-src 'none'", "frame-ancestors 'none'", "sandbox"))));
  }

  @ParameterizedTest
  @MethodSource("data")
  void receiveDefinedHeader(String requiredHeaderName, String requiredHeaderValue) {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("header")
            .request()
            .get()) {
      assertThat(response.getHeaders().get(requiredHeaderName))
          .containsExactly(requiredHeaderValue);
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  void allowOverwriteOfHeader(String requiredHeaderName, String defaultHeaderValue) {
    try (Response response =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("header")
            .queryParam("name", requiredHeaderName)
            .queryParam("value", "CUSTOM_VALUE")
            .request()
            .get()) {
      assertThat(response.getHeaders().get(requiredHeaderName))
          .containsExactly("CUSTOM_VALUE")
          .doesNotContain(defaultHeaderValue);
    }
  }
}
