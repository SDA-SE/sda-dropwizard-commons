package org.sdase.commons.client.jersey.wiremock.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockExtensionTest {

  @RegisterExtension
  WireMockExtension wire = new WireMockExtension(new WireMockConfiguration().dynamicPort(), false);

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
    }
  }

  @Test
  void shouldGet404() throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(wire.baseUrl() + "/foo").openConnection();
    assertThat(connection.getResponseCode()).isEqualTo(404);
  }
}
