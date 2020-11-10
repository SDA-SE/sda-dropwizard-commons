package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricFilter;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;

/** Test that http client configuration is correct. */
public class ApiClientConfigurationTest {

  public static final WireMockRule WIRE =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  public static final DropwizardAppRule<ClientTestConfig> DW =
      new DropwizardAppRule<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockClient.apiBaseUrl", WIRE::baseUrl));

  @ClassRule public static final RuleChain CHAIN = RuleChain.outerRule(WIRE).around(DW);

  private ClientTestApp app;

  @Before
  public void setUp() {
    WIRE.resetAll();
    app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);
  }

  @Test
  public void disableGzip() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setGzipEnabled(false);

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(config)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", notMatching("gzip"))
            .willReturn(ok().withHeader("Content-type", "application/json").withBody("[]")));

    assertThat(client.getCars()).isEmpty();
  }

  @Test
  public void enableGzipForRequests() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setGzipEnabledForRequests(true);

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(config)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .withHeader("Content-Encoding", matching("gzip"))
            .willReturn(created()));

    assertThat(client.createCar(new Car().setSign("HH UV 42")))
        .extracting(Response::getStatus)
        .isEqualTo(SC_CREATED);
  }

  @Test
  public void enableChunkedEncoding() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setChunkedEncodingEnabled(true);

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(config)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    WIRE.stubFor(
        post("/api/cars") // NOSONAR
            .withHeader("Transfer-Encoding", equalTo("chunked"))
            .willReturn(created()));

    assertThat(client.createCar(new Car().setSign("HH UV 42")))
        .extracting(Response::getStatus)
        .isEqualTo(SC_CREATED);
  }

  @Test
  public void shouldNotModifyExistingConfiguration() {
    HttpClientConfiguration config = new HttpClientConfiguration();
    config.setTimeout(Duration.milliseconds(50));
    config.setConnectionTimeout(Duration.milliseconds(50));

    app.getJerseyClientBundle()
        .getClientFactory()
        .externalClient(config)
        .withConnectionTimeout(java.time.Duration.ofDays(1))
        .withReadTimeout(java.time.Duration.ofDays(1))
        .api(MockApiClient.class)
        .atTarget(WIRE.baseUrl());

    assertThat(config.getTimeout()).isEqualTo(Duration.milliseconds(50));
    assertThat(config.getConnectionTimeout()).isEqualTo(Duration.milliseconds(50));
  }
}
