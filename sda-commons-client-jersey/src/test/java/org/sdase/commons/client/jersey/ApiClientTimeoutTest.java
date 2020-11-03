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
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

/** Test that timeouts are correctly mapped. */
public class ApiClientTimeoutTest {

  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final DropwizardAppRule<ClientTestConfig> DW =
      new DropwizardAppRule<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          config("mockBaseUrl", WIRE::baseUrl));

  @ClassRule public static final RuleChain rule = RuleChain.outerRule(WIRE).around(DW);

  @Rule public final RetryRule retryRule = new RetryRule();

  private ClientTestApp app;

  @Before
  public void resetRequests() {
    WIRE.resetRequests();
    app = DW.getApplication();

    // reset the metrics since we don't use it in this test
    DW.getEnvironment().metrics().removeMatching(MetricFilter.ALL);
  }

  @Test
  @Retry(5)
  public void runIntoDefaultConnectionTimeoutOf500Millis() {
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
  @Retry(5)
  public void runIntoConfiguredConnectionTimeout() {
    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .withConnectionTimeout(Duration.ofSeconds(2))
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
  @Retry(5)
  public void runIntoDefaultReadTimeoutOf2Seconds() {
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
  @Retry(5)
  public void runIntoConfiguredReadTimeout() {
    WIRE.stubFor(
        get("/api/cars").willReturn(aResponse().withStatus(200).withBody("").withFixedDelay(5000)));

    MockApiClient client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .withReadTimeout(Duration.ofSeconds(4))
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
