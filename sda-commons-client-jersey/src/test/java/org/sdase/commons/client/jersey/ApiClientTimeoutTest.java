package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.asClientRequestException;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.connectTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.readTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.timeoutError;

import com.codahale.metrics.MetricFilter;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

/** Test that timeouts are correctly mapped. */
class ApiClientTimeoutTest {

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<ClientTestConfig> DW =
      new DropwizardAppExtension<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE::baseUrl));

  private ClientTestApp app;

  @BeforeEach
  void resetRequests() {
    WIRE.resetRequests();
    app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);
  }

  @Test
  @RetryingTest(5)
  void runIntoDefaultConnectionTimeoutOf500Millis() {
    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .api(MockApiClient.class)
            .atTarget("http://192.168.123.123");

    CompletableFuture<List<Car>> future = CompletableFuture.supplyAsync(client::getCars);

    await()
        .untilAsserted(
            () ->
                assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(future::get)
                    .havingCause()
                    .is(asClientRequestException(timeoutError()))
                    .is(asClientRequestException(connectTimeoutError())));
  }

  @Test
  @RetryingTest(5)
  void runIntoConfiguredConnectionTimeout() {
    HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
    clientConfiguration.setConnectionTimeout(io.dropwizard.util.Duration.seconds(2));

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(clientConfiguration)
            .api(MockApiClient.class)
            .atTarget("http://192.168.123.123");

    CompletableFuture<List<Car>> future = CompletableFuture.supplyAsync(client::getCars);

    await()
        .atLeast(1, SECONDS)
        .untilAsserted(
            () ->
                assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(future::get)
                    .havingCause()
                    .is(asClientRequestException(timeoutError()))
                    .is(asClientRequestException(connectTimeoutError())));
  }

  @Test
  @RetryingTest(5)
  void runIntoDefaultReadTimeoutOf2Seconds() {
    WIRE.stubFor(
        get("/api/cars").willReturn(aResponse().withStatus(200).withBody("").withFixedDelay(3000)));

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    CompletableFuture<List<Car>> future = CompletableFuture.supplyAsync(client::getCars);

    await()
        .atLeast(1, SECONDS)
        .untilAsserted(
            () ->
                assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(future::get)
                    .havingCause()
                    .is(asClientRequestException(timeoutError()))
                    .is(asClientRequestException(readTimeoutError())));
  }

  @Test
  @RetryingTest(5)
  void runIntoConfiguredReadTimeout() {
    WIRE.stubFor(
        get("/api/cars").willReturn(aResponse().withStatus(200).withBody("").withFixedDelay(5000)));

    HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
    clientConfiguration.setTimeout(io.dropwizard.util.Duration.seconds(4));
    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(clientConfiguration)
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

    CompletableFuture<List<Car>> future = CompletableFuture.supplyAsync(client::getCars);

    await()
        .atLeast(3, SECONDS)
        .untilAsserted(
            () ->
                assertThatExceptionOfType(ExecutionException.class)
                    .isThrownBy(future::get)
                    .havingCause()
                    .is(asClientRequestException(timeoutError()))
                    .is(asClientRequestException(readTimeoutError())));
  }
}
