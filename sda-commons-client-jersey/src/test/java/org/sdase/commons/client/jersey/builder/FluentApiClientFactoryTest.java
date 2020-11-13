package org.sdase.commons.client.jersey.builder;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.seeOther;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.sdase.commons.client.jersey.ApiHttpClientConfiguration;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.filter.AddRequestHeaderFilter;
import org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder;
import org.sdase.commons.client.jersey.test.ClientTestApp;
import org.sdase.commons.client.jersey.test.ClientTestConfig;
import org.sdase.commons.client.jersey.test.MockApiClient;
import org.sdase.commons.shared.tracing.ConsumerTracing;

public class FluentApiClientFactoryTest {

  private static final AtomicInteger CLIENT_COUNTER = new AtomicInteger();

  private static final WireMockClassRule WIRE = new WireMockClassRule(0);

  private static final DropwizardAppRule<ClientTestConfig> DW =
      new DropwizardAppRule<>(
          ClientTestApp.class,
          resourceFilePath("test-config.yaml"),
          ConfigOverride.config("mockBaseUrl", WIRE::baseUrl));

  @ClassRule public static final TestRule RULE_CHAIN = RuleChain.outerRule(WIRE).around(DW);

  private final ApiHttpClientConfiguration apiClientConfig =
      new ApiHttpClientConfiguration().setApiBaseUrl(WIRE.baseUrl());
  private ClientFactory clientFactory;

  @Before
  public void initSuccessfulResponseForCarsEndpoint() {
    WIRE.resetAll();
    WIRE.addStubMapping(get("/api/cars").willReturn(okForJson(new ArrayList<>())).build());
  }

  @Before
  public void selectClientFactoryFromApplication() {
    ClientTestApp application = DW.getApplication();
    this.clientFactory = application.getJerseyClientBundle().getClientFactory();
  }

  @Test
  public void shouldPassThroughAuthentication() {
    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            .enablePlatformFeatures()
            .enableAuthenticationPassThrough()
            .build(uniqueClientName());

    inRequestContextWithAuthentication(client::getCars);

    WIRE.verify(
        WireMock.getRequestedFor(WireMock.urlEqualTo("/api/cars"))
            .withHeader(HttpHeaders.AUTHORIZATION, matching("^Bearer .*$")));
  }

  @Test
  public void shouldNotPassThroughAuthentication() {
    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            .enablePlatformFeatures()
            // not enableAuthenticationPassThrough
            .build(uniqueClientName());

    inRequestContextWithAuthentication(client::getCars);

    WIRE.verify(
        WireMock.getRequestedFor(WireMock.urlEqualTo("/api/cars"))
            .withoutHeader(HttpHeaders.AUTHORIZATION));
  }

  @Test
  public void shouldSendConsumerToken() {
    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            .enablePlatformFeatures()
            .enableConsumerToken()
            .build(uniqueClientName());

    client.getCars();

    WIRE.verify(
        WireMock.getRequestedFor(WireMock.urlEqualTo("/api/cars"))
            .withHeader(ConsumerTracing.TOKEN_HEADER, matching("^.+$")));
  }

  @Test
  public void shouldNotSendConsumerToken() {
    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            .enablePlatformFeatures()
            // not enableConsumerToken
            .build(uniqueClientName());

    client.getCars();

    WIRE.verify(
        WireMock.getRequestedFor(WireMock.urlEqualTo("/api/cars"))
            .withoutHeader(ConsumerTracing.TOKEN_HEADER));
  }

  @Test
  public void shouldUseCustomFilter() {
    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            .addFilter(
                new AddRequestHeaderFilter() {
                  @Override
                  public String getHeaderName() {
                    return "Custom-Header";
                  }

                  @Override
                  public Optional<String> getHeaderValue() {
                    return Optional.of("example");
                  }
                })
            .build(uniqueClientName());

    client.getCars();

    WIRE.verify(
        WireMock.getRequestedFor(WireMock.urlEqualTo("/api/cars"))
            .withHeader("Custom-Header", equalTo("example")));
  }

  @Test
  public void shouldFollowRedirectsByDefault() {
    MockApiClient.Car testCar = new MockApiClient.Car().setColor("blue").setSign("HH AB 1234");

    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            // not disableFollowRedirects
            .build(uniqueClientName());

    WIRE.addStubMapping(
        post("/api/cars").willReturn(seeOther(WIRE.baseUrl() + "/api/cars/HH-AB-1234")).build());
    WIRE.addStubMapping(get("/api/cars/HH-AB-1234").willReturn(okForJson(testCar)).build());

    try (Response r = client.createCar(testCar)) {
      assertThat(r.getStatus()).isEqualTo(200);
      MockApiClient.Car car = r.readEntity(MockApiClient.Car.class);
      assertThat(car)
          .extracting(MockApiClient.Car::getSign, MockApiClient.Car::getColor)
          .containsExactly("HH AB 1234", "blue");
    }
  }

  @Test
  public void shouldNotFollowRedirects() {
    MockApiClient client =
        clientFactory
            .apiClient(MockApiClient.class)
            .withConfiguration(apiClientConfig)
            .disableFollowRedirects()
            .build(uniqueClientName());

    WIRE.addStubMapping(
        post("/api/cars").willReturn(seeOther(WIRE.baseUrl() + "/api/cars/HH-AB-1234")).build());

    try (Response r =
        client.createCar(new MockApiClient.Car().setColor("blue").setSign("HH AB 1234"))) {
      assertThat(r.getStatus()).isEqualTo(303);
    }
  }

  private void inRequestContextWithAuthentication(Runnable r) {
    ContainerRequestContextHolder containerRequestContextHolder =
        new ContainerRequestContextHolder();
    try {
      ContainerRequestContext containerRequestContextMock =
          mock(ContainerRequestContext.class, RETURNS_DEEP_STUBS);
      MultivaluedMap<String, String> headers = new MultivaluedStringMap();
      headers.add(HttpHeaders.AUTHORIZATION, "Bearer ey.ey.sig");
      when(containerRequestContextMock.getHeaders()).thenReturn(headers);
      containerRequestContextHolder.filter(containerRequestContextMock);
      r.run();
    } finally {
      containerRequestContextHolder.filter(null, null);
    }
  }

  private String uniqueClientName() {
    return MockApiClient.class.getSimpleName()
        + "-"
        + getClass().getSimpleName()
        + "-"
        + CLIENT_COUNTER.incrementAndGet();
  }
}
