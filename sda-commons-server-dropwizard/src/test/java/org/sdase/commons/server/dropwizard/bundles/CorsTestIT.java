package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.dropwizard.bundles.test.CorsAllowTestApp;
import org.sdase.commons.server.dropwizard.bundles.test.CorsDenyTestApp;
import org.sdase.commons.server.dropwizard.bundles.test.CorsRestrictedTestApp;
import org.sdase.commons.server.dropwizard.bundles.test.CorsTestConfiguration;

class CorsTestIT {

  // Request headers
  public static final String ORIGIN_HEADER = "Origin";
  public static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER = "Access-Control-Request-Method";
  // Response headers
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
  public static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
  public static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER =
      "Access-Control-Allow-Credentials";
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS_HEADER = "Access-Control-Expose-Headers";

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_ALLOW =
      new DropwizardAppExtension<>(
          CorsAllowTestApp.class, ResourceHelpers.resourceFilePath("test-config-cors.yaml"));

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_DENY =
      new DropwizardAppExtension<>(
          CorsDenyTestApp.class, ResourceHelpers.resourceFilePath("test-config-cors-deny.yaml"));

  @RegisterExtension
  @Order(2)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_RESTRICTED =
      new DropwizardAppExtension<>(
          CorsRestrictedTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-cors-restricted.yaml"));

  @RegisterExtension
  @Order(3)
  static final DropwizardAppExtension<CorsTestConfiguration> DW_PATTERN =
      new DropwizardAppExtension<>(
          CorsRestrictedTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-cors-pattern.yaml"));

  private static final Function<Integer, String> URL_PATTERN =
      "http://localhost:%d/samples/empty"::formatted;

  private final String allowAllEndpoint = URL_PATTERN.apply(DW_ALLOW.getLocalPort());
  private final String denyEndpoint = URL_PATTERN.apply(DW_DENY.getLocalPort());
  private final String restrictedEndpoint = URL_PATTERN.apply(DW_RESTRICTED.getLocalPort());
  private final String patternEndpoint = URL_PATTERN.apply(DW_PATTERN.getLocalPort());

  @Test
  void shouldNotSetHeaderWhenDeny() {
    try (Response response =
        DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(ORIGIN_HEADER, "server-a.com")
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
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
            .header(ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
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
            .header(ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
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
            .header(ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
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
            .header(ORIGIN_HEADER, origin)
            .header(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
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
            .header(ORIGIN_HEADER, origin)
            .header(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER),
          List.of("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_HEADERS_HEADER).split(","))
          .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
    }
  }

  @Test
  void shouldRespondWithStandardAllowHeaderForNonPreflightOptionsRequest() {
    try (Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header(ORIGIN_HEADER, "some-origin.com")
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
            .header(ORIGIN_HEADER, origin)
            .header(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER), List.of("GET", "POST"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
          .isEqualTo(Boolean.TRUE.toString());
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_HEADERS_HEADER).split(","))
          .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
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
            .header(ORIGIN_HEADER, origin)
            .header(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
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
            .header(ORIGIN_HEADER, origin)
            .header(ACCESS_CONTROL_REQUEST_METHOD_HEADER, "PUT")
            .options()) {

      // Origin is allowed, so header is present
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
          .isEqualTo("server-a.com");

      // The allowed methods header is present, but should NOT contain the requested PUT
      // Since jetty12 browser have to check themselves if the request is allowed in case of
      // preflight
      String[] allowMethods =
          response.getHeaderString(ACCESS_CONTROL_ALLOW_METHODS_HEADER).split(",");
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
            .header(ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER)).isNullOrEmpty();
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
            .header(ORIGIN_HEADER, origin)
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
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
            .header(ORIGIN_HEADER, origin)
            .header(ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .get()) {

      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertStringContainsAllWithoutOrder(
          response.getHeaderString(ACCESS_CONTROL_EXPOSE_HEADERS_HEADER),
          List.of("Location", "exposed"));
      assertThat(response.getHeaderString(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
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
