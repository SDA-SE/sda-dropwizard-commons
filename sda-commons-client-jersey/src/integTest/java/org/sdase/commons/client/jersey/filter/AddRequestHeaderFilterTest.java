package org.sdase.commons.client.jersey.filter;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class AddRequestHeaderFilterTest {

   @ClassRule
   public static final WireMockClassRule WIRE = new WireMockClassRule(wireMockConfig().dynamicPort());

   @Before
   public void resetRequests() {
      WIRE.resetAll();
   }

   @Test
   public void addMultipleHeadersWithAnonymousImplementations() {

      WIRE.stubFor(get("/").willReturn(aResponse().withStatus(200).withBody("")));

      AddRequestHeaderFilter testHeaderFilter = new AddRequestHeaderFilter() {
         @Override
         public String getHeaderName() {
            return "Test";
         }

         @Override
         public Optional<String> getHeaderValue() {
            return Optional.of("dummy");
         }
      };
      AddRequestHeaderFilter fooHeaderFilter = new AddRequestHeaderFilter() {
         @Override
         public String getHeaderName() {
            return "Foo";
         }

         @Override
         public Optional<String> getHeaderValue() {
            return Optional.of("bar");
         }
      };
      WebTarget webTarget = JerseyClientBuilder.createClient()
            .register(testHeaderFilter)
            .register(fooHeaderFilter)
            .target(WIRE.baseUrl());


      webTarget.request().get();

      WIRE.verify(RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/"))
            .withHeader("Test", equalTo("dummy"))
            .withHeader("Foo", equalTo("bar")));
   }

}