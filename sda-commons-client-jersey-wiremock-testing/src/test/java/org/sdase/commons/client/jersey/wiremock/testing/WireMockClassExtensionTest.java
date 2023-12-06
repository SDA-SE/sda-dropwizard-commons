package org.sdase.commons.client.jersey.wiremock.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @deprecated This test is still here to show migration from {@link
 *     org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension} in its commit
 *     history.
 */
@Deprecated(forRemoval = true)
@WireMockTest
class WireMockClassExtensionTest {

  @BeforeEach
  public void addStubs() {
    stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", notMatching("gzip"))
            .willReturn(ok().withHeader("Content-type", "application/json").withBody("[]")));
  }

  @Test
  void shouldGetMockedResponse(WireMockRuntimeInfo wire) throws Exception {
    URLConnection connection = new URL(wire.getHttpBaseUrl() + "/api/cars").openConnection();
    try (InputStream inputStream = connection.getInputStream()) {
      assertThat(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).isEqualTo("[]");
    }
  }

  @Test
  void shouldGet404(WireMockRuntimeInfo wire) throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(wire.getHttpBaseUrl() + "/foo").openConnection();
    assertThat(connection.getResponseCode()).isEqualTo(404);
  }
}
