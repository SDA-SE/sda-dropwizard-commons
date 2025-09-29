package org.sdase.commons.server.cors;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.cors.test.CorsAllowTestApp;
import org.sdase.commons.server.cors.test.CorsDenyTestApp;
import org.sdase.commons.server.cors.test.CorsRestrictedTestApp;
import org.sdase.commons.server.cors.test.CorsTestConfiguration;

class CorsTestIT {

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_ALLOW =
      new DropwizardAppExtension<>(
          CorsAllowTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_DENY =
      new DropwizardAppExtension<>(
          CorsDenyTestApp.class, ResourceHelpers.resourceFilePath("test-config-deny.yaml"));

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_RESTRICTED =
      new DropwizardAppExtension<>(
          CorsRestrictedTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-restricted.yaml"));

  @RegisterExtension
  @Order(3)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_PATTERN =
      new DropwizardAppExtension<>(
          CorsRestrictedTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-pattern.yaml"));

  private final String allowAllEndpoint =
      "http://localhost:" + DW_ALLOW.getLocalPort() + "/samples/empty";
  private final String denyEndpoint =
      "http://localhost:" + DW_DENY.getLocalPort() + "/samples/empty";
  private final String restrictedEndpoint =
      "http://localhost:" + DW_RESTRICTED.getLocalPort() + "/samples/empty";
  private final String patternEndpoint =
      "http://localhost:" + DW_PATTERN.getLocalPort() + "/samples/empty";

  @Test
  void shouldNotSetHeaderWhenDeny() {
    try (Response response =
        DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, "server-a.com")
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldSetHeaderWhenAllow() {
    String origin = "some.com";
    try (Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
    }
  }

  @Test
  void shouldSetHeaderWhenOriginAllowed() {
    String origin = "server-a.com";
    try (Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
    }
  }

  @Test
  void shouldNotSetHeaderWhenOriginNotAllowed() {
    String origin = "server-b.com";
    try (Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldNotSetHeaderWhenDeniedPreflight() {
    String origin = "server-a.com";
    try (Response response =
        DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .header(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldSetHeaderWhenAllowPreflight() {
    String origin = "some.com";
    try (Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .header(CorsHeader.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER),
          List.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
      assertThat(
              response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_HEADERS_HEADER).split(","))
          .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldRespondWithStandardAllowHeaderForNonPreflightOptionsRequest() {
    try (Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, "some-origin.com")
            .options()) {
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(HttpHeaders.ALLOW), List.of("HEAD", "GET", "OPTIONS"));
    }
  }

  @Test
  void shouldSetHeaderWhenOriginAllowedPreflight() {
    String origin = "server-a.com";
    try (Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .header(CorsHeader.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER),
          List.of("GET", "POST"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
      assertThat(
              response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_HEADERS_HEADER).split(","))
          .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldNotSetHeaderWhenOriginNotAllowedPreflight() {
    String origin = "server-b.com";
    try (Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .header(CorsHeader.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldNotSetHeaderWhenMethodNotAllowedPreflight() {
    String origin = "server-a.com";

    try (Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .header(CorsHeader.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "PUT")
            .options()) {

      // Origin is allowed, so header is present
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo("server-a.com");

      // The allowed methods header is present, but should NOT contain the requested PUT
      // Since jetty12 browser have to check themselves if the request is allowed in case of
      // preflight
      String[] allowMethods =
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER).split(",");
      assertThat(allowMethods).contains("POST", "GET");
      assertThat(allowMethods).doesNotContain("PUT");
    }
  }

  @Test
  void shouldNotSetHeaderWhenDenyedUnmatchedHostname() {
    String origin = "unknown-server-a.com";
    try (Response response =
        DW_PATTERN
            .client()
            .target(patternEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
          .isNullOrEmpty();
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isNullOrEmpty();
    }
  }

  @Test
  void shouldSetHeaderWhenAllowForMatchedSubdomain() {
    String origin = "unknown.server-a.com";
    try (Response response =
        DW_PATTERN
            .client()
            .target(patternEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
    }
  }

  @Test
  void shouldSetHeaderWhenAllowForMatchedDomain() {
    String origin = "unknownserver-c.com";
    try (Response response =
        DW_PATTERN
            .client()
            .target(patternEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(CorsHeader.ORIGIN_HEADER, origin)
            .header(CorsHeader.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .get()) {

      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(CorsHeader.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(CorsHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
    }
  }

  private String[] getAllowedHeaderList(String... configured) {
    List<String> allowedHeaders = new ArrayList<>(Arrays.asList(configured));
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("Authorization");
    allowedHeaders.add("X-Requested-With");
    allowedHeaders.add("Accept");
    allowedHeaders.add("Consumer-Token");
    allowedHeaders.add("Trace-Token");
    return allowedHeaders.toArray(new String[0]);
  }

  private void assertStringContainsAllWithoutOrder(String actual, List<String> expected) {
    List<String> actualAsList =
        Arrays.stream(actual.split(",")).map(String::trim).distinct().toList();

    assertThat(actualAsList).containsExactlyInAnyOrder(expected.toArray(new String[0]));
  }
}
