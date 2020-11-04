package org.sdase.commons.server.auth.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.dropwizard.testing.ConfigOverride.config;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.http.HttpHeaders;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.auth.testing.test.AuthTestApp;
import org.sdase.commons.server.auth.testing.test.AuthTestConfig;
import org.sdase.commons.server.testing.EnvironmentRule;
import org.sdase.commons.server.testing.SystemPropertyRule;

/** A test that checks if the jersey client that is used to load keys can use a proxy server */
public class KeyLoaderProxyIT {
  private static final WireMockClassRule PROXY_WIRE =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  private static final EnvironmentRule ENVIRONMENT_RULE =
      new EnvironmentRule().setEnv("AUTH_RULE", "{\"keys\": [{}]}");

  private static final SystemPropertyRule SYSTEM_PROPERTY =
      new SystemPropertyRule()
          .setProperty("http.proxyHost", "localhost")
          .setProperty("http.proxyPort", () -> "" + PROXY_WIRE.port())
          .setProperty("http.nonProxyHosts", "localhost");

  private static final DropwizardAppRule<AuthTestConfig> DW =
      new DropwizardAppRule<>(
          AuthTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"),
          config("auth.keys[0].type", "JWKS"),
          config("auth.keys[0].location", "http://sda.se/jwks"));

  @ClassRule
  public static final RuleChain rule =
      RuleChain.outerRule(PROXY_WIRE).around(ENVIRONMENT_RULE).around(SYSTEM_PROPERTY).around(DW);

  @BeforeClass
  public static void setupWiremock() {
    // expect that the proxy receives the request
    PROXY_WIRE.stubFor(
        get("/jwks")
            .withHeader(HttpHeaders.HOST, equalTo("sda.se"))
            .willReturn(okJson("{\"keys\": []}")));
  }

  @Test
  public void shouldUseProxy() {
    // given
    final String tokenWithUnknownKid =
        "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNHV1RlQUJwWnYyN3RfWDFnTW92NEVlRWhEOXRBMWVhcUgzVzFmMXE4Y28ifQ.eyJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0In0.nHN-k_uvKNl8Nh5lXctQkL8KrWKggGiBQ-jaR0xIq_TAWBbhz5zkGXQTiNZwjPFOIcjyuL1xMCqzLPAKiI0Jy0hwOa4xcqukrWr4UwhKC50dnJiFqUgpGM0xLyT1D8JKdSNiVtYL0k-E5XCcpDEqOjHOG3Gw03VoZ0iRNeU2X49Rko8646l5j2g4QbuuOSn1a5G4ICMCAY7C6Vb55dgJtG_WAvkhFdBd_ShQEp_XfWJh6uq0E95_8yfzBx4UuK1Q-TLuWrXKxOlYNCuCH90NYG-3oF9w0gFtdXtYOFzPIEVIkU0Ra6sk_s0IInrEMD_3Q4fgE2PqOzqpuVaD_lHdAA";

    // when
    Response response =
        createWebTarget()
            .path("/secure") // NOSONAR
            .request(APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer " + tokenWithUnknownKid)
            .get();

    // then
    assertThat(response.getStatus()).isEqualTo(SC_UNAUTHORIZED);

    // one call on startup, one call from the request
    PROXY_WIRE.verify(
        2,
        RequestPatternBuilder.newRequestPattern(RequestMethod.GET, urlEqualTo("/jwks"))
            .withHeader(HttpHeaders.HOST, equalTo("sda.se")));
  }

  private WebTarget createWebTarget() {
    return DW.client().target("http://localhost:" + DW.getLocalPort());
  }
}
