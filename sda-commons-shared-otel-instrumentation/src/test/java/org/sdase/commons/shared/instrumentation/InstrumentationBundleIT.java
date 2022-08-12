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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

class InstrumentationBundleIT {
  private DropwizardTestSupport<Configuration> DW;

  @BeforeEach
  void setUp() throws Exception {
    DW = new DropwizardTestSupport<>(TestApp.class, null, randomPorts());
    DW.before();
  }

  @Test
  @SetEnvironmentVariable(key = "MAIN_THREAD_CHECK_ENABLED", value = "false")
  @SetEnvironmentVariable(key = "JAEGER_SAMPLER_TYPE", value = "const")
  @SetEnvironmentVariable(key = "JAEGER_SAMPLER_PARAM", value = "0")
  void shouldNotLoadForLegacyParams() throws IOException {
    List<Header> headers = makeHttpCallAndReturnHeaders();
    assertThat(System.getenv("JAEGER_SAMPLER_TYPE")).isEqualTo("const");
    assertThat(System.getenv("JAEGER_SAMPLER_PARAM")).isEqualTo("0");
    assertThat(headers).isEmpty();
  }

  @Test
  @SetEnvironmentVariable(key = "MAIN_THREAD_CHECK_ENABLED", value = "false")
  @SetEnvironmentVariable(key = "RUNTIME_ATTACH_ENABLED", value = "false")
  void shouldNotLoadForRunTimeDisabled() throws IOException {
    List<Header> headers = makeHttpCallAndReturnHeaders();
    assertThat(System.getenv("RUNTIME_ATTACH_ENABLED")).isEqualTo("false");
    assertThat(headers).isEmpty();
  }

  @Test
  @SetEnvironmentVariable(key = "MAIN_THREAD_CHECK_ENABLED", value = "false")
  @SetEnvironmentVariable(key = "OTEL_JAVAAGENT_ENABLED", value = "false")
  void shouldNotLoad() throws IOException {
    List<Header> headers = makeHttpCallAndReturnHeaders();
    assertThat(System.getenv("JAEGER_SAMPLER_TYPE")).isBlank();
    assertThat(System.getenv("JAEGER_SAMPLER_PARAM")).isBlank();
    assertThat(System.getenv("OTEL_JAVAAGENT_ENABLED")).isEqualTo("false");
    assertThat(headers).isEmpty();
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
