package org.sdase.commons.client.jersey.proxy;

import static io.dropwizard.testing.ConfigOverride.randomPorts;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.client.proxy.ProxyConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.HttpClientConfiguration;
import org.sdase.commons.client.jersey.JerseyClientBundle;

class ClientProxyTestSetup {

  // dummy.server.test must be added to /etc/hosts and point to 127.0.0.1 for this test
  public static final String SERVER_DOMAIN = "dummy.server.test";

  static boolean hostNotSet() {
    try {
      String hostAddressDummyDomain = InetAddress.getByName(SERVER_DOMAIN).getHostAddress();
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
  static final DropwizardAppExtension<ClientProxyTestConfig> DW =
      new DropwizardAppExtension<>(ClientProxyTestApp.class, null, randomPorts());

  @BeforeEach
  void respondAnythingWithPong() {
    WIRE_MOCK.resetAll();
    WIRE_MOCK.stubFor(
        WireMock.get(WireMock.anyUrl()).willReturn(WireMock.jsonResponse("\"pong\"", 200)));
  }

  protected String pingRequest() {
    return getClientTarget()
        .path("ping")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get(String.class);
  }

  protected WebTarget getClientTarget() {
    ClientProxyTestApp app = DW.getApplication();
    return app.getClientTarget();
  }

  public static class ClientProxyTestConfig extends Configuration {
    private HttpClientConfiguration client = createConfig();

    public HttpClientConfiguration getClient() {
      return client;
    }

    public ClientProxyTestConfig setClient(HttpClientConfiguration client) {
      this.client = client;
      return this;
    }

    private static HttpClientConfiguration createConfig() {
      var config = new HttpClientConfiguration();
      if ("true".equalsIgnoreCase(System.getProperty("configureClientTestProxy"))) {
        var proxyConfig = new ProxyConfiguration();
        proxyConfig.setHost("nowhere.example.com");
        config.setProxyConfiguration(proxyConfig);
      }
      if ("true".equalsIgnoreCase(System.getProperty("configureClientTestProxyWithExcludedHost"))) {
        var proxyConfig = new ProxyConfiguration();
        proxyConfig.setHost("nowhere.example.com");
        proxyConfig.setNonProxyHosts(List.of(SERVER_DOMAIN));
        config.setProxyConfiguration(proxyConfig);
      }
      return config;
    }
  }

  public static class ClientProxyTestApp extends Application<ClientProxyTestConfig> {

    private final JerseyClientBundle<Configuration> clients = JerseyClientBundle.builder().build();

    private WebTarget clientTarget;

    @Override
    public void initialize(Bootstrap<ClientProxyTestConfig> bootstrap) {
      super.initialize(bootstrap);
      bootstrap.addBundle(this.clients);
    }

    @Override
    public void run(ClientProxyTestConfig clientProxyTestConfig, Environment environment) {
      this.clientTarget =
          clients
              .getClientFactory()
              .platformClient(clientProxyTestConfig.getClient())
              .buildGenericClient(UUID.randomUUID().toString())
              .target(WIRE_MOCK.baseUrl().replace("localhost", SERVER_DOMAIN));
    }

    public WebTarget getClientTarget() {
      return clientTarget;
    }
  }
}
