package org.sdase.commons.client.jersey.wiremock.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockClassExtensionTest {

  @RegisterExtension public static WireMockClassExtension wire = new WireMockClassExtension();

  @BeforeAll
  public static void beforeAll() {
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
    }
  }

  @Test
  void shouldGet404() throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(wire.baseUrl() + "/foo").openConnection();
    assertThat(connection.getResponseCode()).isEqualTo(404);
  }

  @Nested
  class ConstructorTests {
    @Test
    void shouldSetupMockForPortAndHttpsPort() {
      WireMockClassExtension wireMockClassExtension = new WireMockClassExtension(8090, 8091);
      assertThat(wireMockClassExtension.getOptions().portNumber()).isEqualTo(8090);
      assertThat(wireMockClassExtension.getOptions().httpsSettings().port()).isEqualTo(8091);
    }

    @Test
    void shouldSetupMockForPort() {
      WireMockClassExtension wireMockClassExtension = new WireMockClassExtension(8090);
      assertThat(wireMockClassExtension.getOptions().portNumber()).isEqualTo(8090);
    }

    @Test
    void shouldSetupMockForNoParameters() {
      WireMockClassExtension wireMockClassExtension = new WireMockClassExtension();
      assertThat(wireMockClassExtension.getOptions().portNumber()).isBetween(0, 9999);
    }
  }
}
