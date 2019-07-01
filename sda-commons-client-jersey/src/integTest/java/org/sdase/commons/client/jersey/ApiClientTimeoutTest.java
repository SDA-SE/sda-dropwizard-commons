package org.sdase.commons.client.jersey;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.server.testing.EnvironmentRule;

import java.time.Duration;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.connectTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.readTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.timeoutError;

/**
 * Test that timeouts are correctly mapped.
 */
public class ApiClientTimeoutTest {

   @ClassRule
   public static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   private final DropwizardAppRule<ClientTestConfig> dw = new DropwizardAppRule<>(
         ClientTestApp.class, resourceFilePath("test-config.yaml"));

   @Rule
   public final RuleChain rule = RuleChain
         .outerRule(new RetryRule())
         .around(new EnvironmentRule().setEnv("MOCK_BASE_URL", WIRE.baseUrl()))
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

      MockApiClient client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .api(MockApiClient.class)
            .atTarget("http://192.168.123.123");

      await()
            .between(400, MILLISECONDS, 600, MILLISECONDS)
            .pollDelay(20, MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(ClientRequestException.class)
                  .isThrownBy(client::getCars)
                  .is(timeoutError())
                  .is(connectTimeoutError()));

   }

   @Test
   @Retry(5)
   public void runIntoConfiguredConnectionTimeout() {
      MockApiClient client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .withConnectionTimeout(Duration.ofMillis(10))
            .api(MockApiClient.class)
            .atTarget("http://192.168.123.123");

      await()
            .between(5, MILLISECONDS, 150, MILLISECONDS)
            .pollDelay(2, MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(ClientRequestException.class)
                  .isThrownBy(client::getCars)
                  .is(timeoutError())
                  .is(connectTimeoutError()));
   }

   @Test
   @Retry(5)
   public void runIntoDefaultReadTimeoutOf2Seconds() {

      WIRE.stubFor(get("/api/cars")
            .willReturn(aResponse()
                  .withStatus(200)
                  .withBody("")
                  .withFixedDelay(3000)
            )
      );

      MockApiClient client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

      await()
            .between(1900, MILLISECONDS, 2200, MILLISECONDS)
            .pollDelay(20, MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(ClientRequestException.class)
                  .isThrownBy(client::getCars)
                  .is(timeoutError())
                  .is(readTimeoutError()));
   }

   @Test
   @Retry(5)
   public void runIntoConfiguredReadTimeoutOf100Millis() {

      WIRE.stubFor(get("/api/cars")
            .willReturn(aResponse()
                  .withStatus(200)
                  .withBody("")
                  .withFixedDelay(300)
            )
      );

      MockApiClient client = app.getJerseyClientBundle().getClientFactory()
            .externalClient()
            .withReadTimeout(Duration.ofMillis(100))
            .api(MockApiClient.class)
            .atTarget(WIRE.baseUrl());

      // the proxy seems to handle timeouts slower than the generic client
      await()
            .between(40, MILLISECONDS, 900, MILLISECONDS)
            .pollDelay(2, MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(ClientRequestException.class)
                  .isThrownBy(client::getCars)
                  .is(timeoutError())
                  .is(readTimeoutError()));
   }


}
