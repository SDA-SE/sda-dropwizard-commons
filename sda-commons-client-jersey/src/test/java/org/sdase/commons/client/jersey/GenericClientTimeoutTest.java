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

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;
import org.sdase.commons.server.testing.SystemPropertyRule;

/** Tests that timeouts are correctly mapped. */
public class GenericClientTimeoutTest {

  @ClassRule
  public static final WireMockClassRule WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private final DropwizardAppRule<ClientTestConfig> dw =
      new DropwizardAppRule<>(ClientTestApp.class, resourceFilePath("test-config.yaml"));

  @Rule
  public final RuleChain rule =
      RuleChain.outerRule(new RetryRule())
          .around(new SystemPropertyRule().setProperty("MOCK_BASE_URL", WIRE.baseUrl()))
          .around(dw);

  private ClientTestApp app;

  @Before
  public void resetRequests() {
    WIRE.resetRequests();
    app = dw.getApplication();
  }

  @Test
  @Retry(5)
  public void runIntoDefaultConnectionTimeoutOf500Millis() {
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
  @Retry(5)
  public void runIntoConfiguredConnectionTimeout() {
    Client client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .withConnectionTimeout(Duration.ofSeconds(2))
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
  @Retry(5)
  public void runIntoDefaultReadTimeoutOf2Seconds() {
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
  @Retry(5)
  public void runIntoConfiguredReadTimeout() {

    WIRE.stubFor(
        get("/timeout").willReturn(aResponse().withStatus(200).withBody("").withFixedDelay(5000)));

    Client client =
        app.getJerseyClientBundle()
            .getClientFactory()
            .externalClient()
            .withReadTimeout(Duration.ofSeconds(4))
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
