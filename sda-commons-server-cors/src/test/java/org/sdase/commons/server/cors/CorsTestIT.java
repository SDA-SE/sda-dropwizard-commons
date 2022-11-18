package org.sdase.commons.server.cors;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.cors.test.CorsAllowTestApp;
import org.sdase.commons.server.cors.test.CorsDenyTestApp;
import org.sdase.commons.server.cors.test.CorsRestrictedTestApp;
import org.sdase.commons.server.cors.test.CorsTestConfiguration;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import org.sdase.commons.shared.tracing.RequestTracing;

class CorsTestIT {

  @RegisterExtension
  @Order(0)
  private static final DropwizardAppExtension<CorsTestConfiguration> DW_ALLOW =
      new DropwizardAppExtension<>(
          CorsAllowTestApp.class, ResourceHelpers.resourceFilePath("test-config.yaml"));

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<CorsTestConfiguration> DW_DENY =
      new DropwizardAppExtension<>(
          CorsDenyTestApp.class, ResourceHelpers.resourceFilePath("test-config-deny.yaml"));

  @RegisterExtension
  @Order(2)
  private static final DropwizardAppExtension<CorsTestConfiguration> DW_RESTRICTED =
      new DropwizardAppExtension<>(
          CorsRestrictedTestApp.class,
          ResourceHelpers.resourceFilePath("test-config-restricted.yaml"));

  @RegisterExtension
  @Order(3)
  private static final DropwizardAppExtension<CorsTestConfiguration> DW_PATTERN =
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
    Response response =
        DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", "server-a.com")
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldSetHeaderWhenAllow() {
    String origin = "some.com";
    Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isEqualTo(origin);
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isEqualTo("Location,exposed");
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isEqualTo(Boolean.TRUE.toString());
  }

  @Test
  void shouldSetHeaderWhenOriginAllowed() {
    String origin = "server-a.com";
    Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isEqualTo(origin);
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isEqualTo("Location,exposed");
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isEqualTo(Boolean.TRUE.toString());
  }

  @Test
  void shouldNotSetHeaderWhenOriginNotAllowed() {
    String origin = "server-b.com";
    Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldNotSetHeaderWhenDenyedPreflight() {
    String origin = "server-a.com";
    Response response =
        DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .options();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldSetHeaderWhenAllowPreflight() {
    String origin = "some.com";
    Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isEqualTo(origin);
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isEqualTo("HEAD,GET,POST,PUT,DELETE,PATCH");
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isEqualTo(Boolean.TRUE.toString());
    assertThat(
            response
                .getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER)
                .split(","))
        .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldRespondWithStandardAllowHeaderForNonPreflightOptionsRequest() {
    Response response =
        DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", "some-origin.com")
            .options();
    assertThat(response.getHeaderString(HttpHeaders.ALLOW)).isEqualTo("HEAD,GET,OPTIONS");
  }

  @Test
  void shouldSetHeaderWhenOriginAllowedPreflight() {
    String origin = "server-a.com";
    Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isEqualTo(origin);
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isEqualTo("GET,POST");
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isEqualTo(Boolean.TRUE.toString());
    assertThat(
            response
                .getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER)
                .split(","))
        .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldNotSetHeaderWhenOriginNotAllowedPreflight() {
    String origin = "server-b.com";
    Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldNotSetHeaderWhenMethodNotAllowedPreflight() {
    String origin = "server-a.com";
    Response response =
        DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "PUT")
            .options();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldNotSetHeaderWhenDenyedUnmatchedHostname() {
    String origin = "unknown-server-a.com";
    Response response =
        DW_PATTERN
            .client()
            .target(patternEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isNullOrEmpty();
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isNullOrEmpty();
  }

  @Test
  void shouldSetHeaderWhenAllowForMatchedSubdomain() {
    String origin = "unknown.server-a.com";
    Response response =
        DW_PATTERN
            .client()
            .target(patternEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isEqualTo(origin);
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isEqualTo("Location,exposed");
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isEqualTo(Boolean.TRUE.toString());
  }

  @Test
  void shouldSetHeaderWhenAllowForMatchedDomain() {
    String origin = "unknownserver-c.com";
    Response response =
        DW_PATTERN
            .client()
            .target(patternEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .get();

    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER))
        .isEqualTo(origin);
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
        .isEqualTo("Location,exposed");
    assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
        .isEqualTo(Boolean.TRUE.toString());
  }

  private String[] getAllowedHeaderList(String... configured) {
    List<String> allowedHeaders = new ArrayList<>(Arrays.asList(configured));
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("Authorization");
    allowedHeaders.add("X-Requested-With");
    allowedHeaders.add("Accept");
    allowedHeaders.add(ConsumerTracing.TOKEN_HEADER);
    allowedHeaders.add(RequestTracing.TOKEN_HEADER);
    return allowedHeaders.toArray(new String[0]);
  }
}
