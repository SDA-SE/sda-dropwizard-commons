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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InstrumentationBundleIT {
  private DropwizardTestSupport<Configuration> DW;
  private static final String MAIN_METHOD_CHECK_PROP =
      "otel.javaagent.testing.runtime-attach.main-method-check";

  @BeforeAll
  static void disableMainThreadCheck() {
    System.setProperty(MAIN_METHOD_CHECK_PROP, "false");
  }

  @BeforeEach
  void setUp() throws Exception {
    DW = new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
  }

  @Test
  @Order(0)
  @SetEnvironmentVariable(key = "JAEGER_SAMPLER_TYPE", value = "const")
  @SetEnvironmentVariable(key = "JAEGER_SAMPLER_PARAM", value = "0")
  void shouldNotLoadForLegacyParams() throws IOException {
    List<Header> headers = makeHttpCallAndReturnHeaders();
    assertThat(System.getenv("JAEGER_SAMPLER_TYPE")).isEqualTo("const");
    assertThat(System.getenv("JAEGER_SAMPLER_PARAM")).isEqualTo("0");
    assertThat(headers).isEmpty();
  }

  @Test
  @Order(1)
  @SetEnvironmentVariable(key = "OTEL_JAVAAGENT_ENABLED", value = "false")
  void shouldNotLoad() throws IOException {
    List<Header> headers = makeHttpCallAndReturnHeaders();
    assertThat(System.getenv("JAEGER_SAMPLER_TYPE")).isBlank();
    assertThat(System.getenv("JAEGER_SAMPLER_PARAM")).isBlank();
    assertThat(System.getenv("OTEL_JAVAAGENT_ENABLED")).isEqualTo("false");
    assertThat(headers).isEmpty();
  }

  @Test
  @Order(2)
  @SetEnvironmentVariable(key = "OTEL_TRACES_EXPORTER", value = "logging")
  @SetEnvironmentVariable(key = "OTEL_JAVAAGENT_DEBUG", value = "true")
  @SetEnvironmentVariable(key = "OTEL_INSTRUMENTATION_APACHE_HTTPCLIENT_ENABLED", value = "true")
  @SetEnvironmentVariable(key = "OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED", value = "false")
  @SetSystemProperty(key = MAIN_METHOD_CHECK_PROP, value = "false")
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
