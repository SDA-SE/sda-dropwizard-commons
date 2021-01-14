package org.sdase.commons.client.jersey.wiremock.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.VerificationException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockExtensionTest {

  @RegisterExtension WireMockExtension wire = new WireMockExtension();

  @BeforeEach
  void before() {
    wire.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", notMatching("gzip"))
            .willReturn(ok().withHeader("Content-type", "application/json").withBody("[]")));
  }

  @Test
  void shouldGetMockedResponse() throws Exception {
    URLConnection connection = new URL(wire.baseUrl() + "/api/cars").openConnection();
    try (InputStream inputStream = connection.getInputStream()) {
      assertThat(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).isEqualTo("[]");
      wire.assertAllRequestsMatched();
    }
  }

  @Test
  void shouldGet404() throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(wire.baseUrl() + "/foo").openConnection();
    assertThat(connection.getResponseCode()).isEqualTo(404);
    assertThrows(
        VerificationException.class,
        wire::assertAllRequestsMatched,
        "Expected assertAllRequestsMatched to throw VerificationException, but it didn't");
  }

  @Nested
  class ConstructorTests {
    @Test
    void shouldSetupMockForPortAndHttpsPort() {
      WireMockExtension wireMockClassExtension = new WireMockExtension(8090, 8091);
      assertThat(wireMockClassExtension.getOptions().portNumber()).isEqualTo(8090);
      assertThat(wireMockClassExtension.getOptions().httpsSettings().port()).isEqualTo(8091);
    }

    @Test
    void shouldSetupMockForPort() {
      WireMockExtension wireMockClassExtension = new WireMockExtension(8090);
      assertThat(wireMockClassExtension.getOptions().portNumber()).isEqualTo(8090);
    }

    @Test
    void shouldSetupMockForNoParameters() {
      WireMockExtension wireMockClassExtension = new WireMockExtension();
      assertThat(wireMockClassExtension.getOptions().portNumber()).isBetween(0, 9999);
    }
  }
}
