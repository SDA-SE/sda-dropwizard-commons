package org.sdase.commons.client.jersey;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.sdase.commons.client.jersey.error.ClientErrorUtil.convertExceptions;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.asClientRequestException;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.connectTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.readTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.timeoutError;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

/** Tests that timeouts are correctly mapped. */
class GenericClientTimeoutTest {

  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<ClientTestConfig> dw =
      new DropwizardAppExtension<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  private ClientTestApp app;

  @BeforeEach
  void resetRequests() {
    WIRE.resetRequests();
    app = dw.getApplication();
    // removing client metrics to allow creation of new clients with same id
    dw.getEnvironment().metrics().removeMatching((name, metric) -> name.contains(".test."));
  }

  @Test
  @RetryingTest(5)
  void runIntoDefaultConnectionTimeoutOf500Millis() {
    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");

    CompletableFuture<Response> future =
        CompletableFuture.supplyAsync(
            () ->
                // try to connect to an ip address that is not routable
                convertExceptions(
                    () -> client.target("http://192.168.123.123").path("timeout").request().get()));

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
    Client client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(clientConfiguration)
            .buildGenericClient("test");

    CompletableFuture<Response> future =
        CompletableFuture.supplyAsync(
            () ->
                // try to connect to an ip address that is not routable
                convertExceptions(
                    () -> client.target("http://192.168.123.123").path("timeout").request().get()));

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
        get("/timeout").willReturn(aResponse().withStatus(200).withBody("").withFixedDelay(3000)));

    Client client =
        app.getJerseyClientBundle().getClientFactory().externalClient().buildGenericClient("test");

    CompletableFuture<Response> future =
        CompletableFuture.supplyAsync(
            () ->
                // try to connect to an ip address that is not routable
                convertExceptions(
                    () -> client.target(WIRE.baseUrl()).path("timeout").request().get()));

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
        get("/timeout").willReturn(aResponse().withStatus(200).withBody("").withFixedDelay(5000)));

    HttpClientConfiguration clientConfiguration = new HttpClientConfiguration();
    clientConfiguration.setTimeout(io.dropwizard.util.Duration.seconds(4));
    Client client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient(clientConfiguration)
            .buildGenericClient("test");

    CompletableFuture<Response> future =
        CompletableFuture.supplyAsync(
            () ->
                // try to connect to an ip address that is not routable
                convertExceptions(
                    () -> client.target(WIRE.baseUrl()).path("timeout").request().get()));

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
