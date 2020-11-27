package org.sdase.commons.server.opa;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.testing.Retry;
import org.sdase.commons.server.testing.RetryRule;

public class OpaBundleClientConfigurationIT {
  public static final WireMockRule WIRE =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  private static final DropwizardAppRule<TestConfiguration> DW =
      new DropwizardAppRule<>(
          TestApplication.class,
          null,
          randomPorts(),
          config("opa.baseUrl", WIRE::baseUrl),
          config("opa.policyPackage", "test"),
          config("opa.opaClient.userAgent", "my-user-agent"),

          // relax the timeout to make tests more stable
          config("opa.opaClient.timeout", "1s"));

  @ClassRule public static RuleChain RULE = RuleChain.outerRule(WIRE).around(DW);

  @Rule public final RetryRule retryRule = new RetryRule();

  @BeforeClass
  public static void beforeClass() {
    WIRE.resetAll();
    WIRE.stubFor(post(anyUrl()).willReturn(okJson("{\"result\": {\"allow\": true}}")));
  }

  @Test
  @Retry(5)
  public void shouldSendCustomUserAgentInTheOpaRequest() {
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
