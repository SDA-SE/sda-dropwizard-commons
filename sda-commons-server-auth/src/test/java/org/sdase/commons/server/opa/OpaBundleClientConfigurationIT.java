package org.sdase.commons.server.opa;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.sdase.commons.server.opa.config.OpaConfig;

class OpaBundleClientConfigurationIT {
  @RegisterExtension
  @Order(0)
  private static final WireMockClassExtension WIRE =
      new WireMockClassExtension(wireMockConfig().dynamicPort());

  ;

  @RegisterExtension
  @Order(1)
  private static final DropwizardAppExtension<TestConfiguration> DW =
      new DropwizardAppExtension<>(
          TestApplication.class,
          null,
          randomPorts(),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "test"),
          config("opa.opaClient.userAgent", "my-user-agent"),
          // relax the timeout to make tests more stable
          config("opa.opaClient.timeout", "1s"));

  @BeforeAll
  public static void beforeAll() {
    WIRE.resetAll();
    WIRE.stubFor(post(anyUrl()).willReturn(okJson("{\"result\": {\"allow\": true}}")));
  }

  @Test
  @RetryingTest(5)
  void shouldSendCustomUserAgentInTheOpaRequest() {
    Response response = createWebTarget().request(APPLICATION_JSON).get();

    assertThat(response.getStatus()).isEqualTo(SC_OK);

    WIRE.verify(
        postRequestedFor(urlEqualTo("/v1/data/test"))
            .withHeader(USER_AGENT, equalTo("my-user-agent")));
  }

  private WebTarget createWebTarget() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }

  public static class TestConfiguration extends Configuration {
    @Valid private OpaConfig opa = new OpaConfig();

    public OpaConfig getOpa() {
      return opa;
    }

    public TestConfiguration setOpa(OpaConfig opa) {
      this.opa = opa;
      return this;
    }
  }

  @Path("")
  public static class TestApplication extends Application<TestConfiguration> {
    final OpaBundle<TestConfiguration> opaBundle =
        OpaBundle.builder().withOpaConfigProvider(TestConfiguration::getOpa).build();

    @Override
    public void initialize(Bootstrap<TestConfiguration> bootstrap) {
      bootstrap.addBundle(opaBundle);
    }

    @Override
    public void run(TestConfiguration configuration, Environment environment) {
      environment.jersey().register(this);
    }

    @GET
    @Produces(APPLICATION_JSON)
    public Map<String, Object> get() {
      return new HashMap<>();
    }
  }
}
