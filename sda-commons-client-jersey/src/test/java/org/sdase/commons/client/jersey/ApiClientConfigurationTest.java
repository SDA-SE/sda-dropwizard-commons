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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;

/** Test that http client configuration is correct. */
public class ApiClientConfigurationTest {

  @ClassRule
  public static final WireMockRule WIRE =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  @Rule
  public final DropwizardAppRule<ClientTestConfig> dw =
      new DropwizardAppRule<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE.baseUrl()));

  private ClientTestApp app;

  @Before
  public void setUp() {
    WIRE.resetAll();
    app = dw.getApplication();
  }

  @Test
  public void disableGzip() {
    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(new HttpClientConfiguration().setGzipEnabled(false))
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
    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(new HttpClientConfiguration().setGzipEnabledForRequests(true))
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
    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(new HttpClientConfiguration().setChunkedEncodingEnabled(true))
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
}
