package org.sdase.commons.server.opa.proxy;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.client.proxy.ProxyConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.config.OpaConfig;

class OpaBundleProxyTestSetup {

  // dummy.opa.test must be added to /etc/hosts and point to 127.0.0.1 for this test
  public static final String OPA_DOMAIN = "dummy.opa.test";

  static boolean hostNotSet() {
    try {
      String hostAddressDummyDomain = InetAddress.getByName(OPA_DOMAIN).getHostAddress();
      return !"127.0.0.1".equalsIgnoreCase(hostAddressDummyDomain);
    } catch (UnknownHostException e) {
      return true;
    }
  }

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance().build();

  @RegisterExtension
  @Order(1)
  static final DropwizardAppExtension<OpaBundleProxyTestConfig> DW =
      new DropwizardAppExtension<>(
          OpaBundleProxyTestApp.class,
          null,
          randomPorts(),
          config("opa.baseUrl", () -> WIRE_MOCK.baseUrl().replace("localhost", OPA_DOMAIN)));

  @BeforeEach
  void allowAllRequestsInOpa() {
    WIRE_MOCK.resetAll();
    WIRE_MOCK.stubFor(
        WireMock.post(WireMock.anyUrl())
            .willReturn(WireMock.jsonResponse(Map.of("result", Map.of("allow", true)), 200)));
  }

  protected String pingRequest() {
    return DW.client()
        .target("http://localhost:%d".formatted(DW.getLocalPort()))
        .path("ping")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(String.class);
  }

  public static class OpaBundleProxyTestConfig extends Configuration {
    private OpaConfig opa = createConfig();

    public OpaConfig getOpa() {
      return opa;
    }

    public OpaBundleProxyTestConfig setOpa(OpaConfig opa) {
      this.opa = opa;
      return this;
    }

    private static OpaConfig createConfig() {
      var config = new OpaConfig();
      if ("true".equalsIgnoreCase(System.getProperty("configureOpaTestProxy"))) {
        var proxyConfig = new ProxyConfiguration();
        proxyConfig.setHost("nowhere.example.com");
        config.getOpaClient().setProxyConfiguration(proxyConfig);
      }
      if ("true".equalsIgnoreCase(System.getProperty("configureOpaTestProxyWithExcludedHost"))) {
        var proxyConfig = new ProxyConfiguration();
        proxyConfig.setHost("nowhere.example.com");
        proxyConfig.setNonProxyHosts(List.of(OPA_DOMAIN));
        config.getOpaClient().setProxyConfiguration(proxyConfig);
      }
      return config;
    }
  }

  public static class OpaBundleProxyTestApp extends Application<OpaBundleProxyTestConfig> {

    @Override
    public void initialize(Bootstrap<OpaBundleProxyTestConfig> bootstrap) {
      super.initialize(bootstrap);
      var opaBundle =
          OpaBundle.builder().withOpaConfigProvider(OpaBundleProxyTestConfig::getOpa).build();
      bootstrap.addBundle(opaBundle);
    }

    @Override
    public void run(OpaBundleProxyTestConfig opaBundleProxyTestConfig, Environment environment) {
      environment.jersey().register(new PingEndpoint());
    }
  }

  @Path("/")
  public static class PingEndpoint {
    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public String ping() {
      return "pong";
    }
  }
}
