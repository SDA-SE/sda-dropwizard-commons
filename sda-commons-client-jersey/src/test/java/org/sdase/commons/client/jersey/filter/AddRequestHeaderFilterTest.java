package org.sdase.commons.client.jersey.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import jakarta.ws.rs.client.WebTarget;
import java.util.Optional;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AddRequestHeaderFilterTest {

  @RegisterExtension
  static final WireMockExtension WIRE = new WireMockExtension.Builder().options(wireMockConfig().dynamicPort()).build();

  @BeforeEach
  void resetRequests() {
    WIRE.resetAll();
  }

  @Test
  void addMultipleHeadersWithAnonymousImplementations() {

    WIRE.stubFor(get("/").willReturn(aResponse().withStatus(200).withBody("")));

    AddRequestHeaderFilter testHeaderFilter =
        new AddRequestHeaderFilter() {
          @Override
          public String getHeaderName() {
            return "Test";
          }

          @Override
          public Optional<String> getHeaderValue() {
            return Optional.of("dummy");
          }
        };
    AddRequestHeaderFilter fooHeaderFilter =
        new AddRequestHeaderFilter() {
          @Override
          public String getHeaderName() {
            return "Foo";
          }

          @Override
          public Optional<String> getHeaderValue() {
            return Optional.of("bar");
          }
        };
    WebTarget webTarget =
        JerseyClientBuilder.createClient()
            .register(testHeaderFilter)
            .register(fooHeaderFilter)
            .target(WIRE.baseUrl());

    webTarget.request().get();

    WIRE.verify(
        RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/"))
            .withHeader("Test", equalTo("dummy"))
            .withHeader("Foo", equalTo("bar")));
  }
}
