package org.sdase.commons.shared.instrumentation;

import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstrumentationBundleIT {
  private DropwizardTestSupport<Configuration> DW;

  @BeforeEach
  void setUp() throws Exception {
    DW = new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
  }

  @AfterEach
  void tearDown() {}

  @Test
  void shouldUseDefaultHeaders() throws IOException {
    List<Header> headers = makeHttpCallAndReturnHeaders();
    // jaeger headers should be used by default along with W3C headers.
    assertThat(headers)
        .extracting(Header::getName)
        .containsExactlyInAnyOrder("traceparent", "uber-trace-id");
  }

  private static List<Header> makeHttpCallAndReturnHeaders() throws IOException {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet request = new HttpGet("https://httpbin.org/get");
      try (CloseableHttpResponse ignored = httpClient.execute(request)) {
        return Arrays.stream(request.getHeaders()).collect(Collectors.toList());
      }
    }
  }
}
