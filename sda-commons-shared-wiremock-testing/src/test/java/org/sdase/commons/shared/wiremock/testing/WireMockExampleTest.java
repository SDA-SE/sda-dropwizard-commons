package org.sdase.commons.shared.wiremock.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.shared.wiremock.testing.testapp.WireMockConfig;
import org.sdase.commons.shared.wiremock.testing.testapp.WireMockTestApp;

class WireMockExampleTest {

  private static final String PATH_OK = "/testOk";
  private static final String PATH_NOT_OK = "/testNotOk";

  @RegisterExtension
  @Order(0)
  static final WireMockExtension WIRE =
      new WireMockExtension.Builder().build();

  @Order(1)
  @RegisterExtension
  static final DropwizardAppExtension<WireMockConfig> DW =
      new DropwizardAppExtension<>(WireMockTestApp.class);

  @BeforeEach
  void setUp() {
    WIRE.stubFor(get(PATH_OK).willReturn(okJson("{\"key\": \"value\"}")));
    WIRE.stubFor(get(PATH_NOT_OK).willReturn(aResponse().withStatus(404)));
  }

  @Test
  void wiremockTestOk() {
    Response response =
        DW.client().target("http://localhost:" + WIRE.getPort() + PATH_OK).request().get();

    Map<String, String> map = response.readEntity(Map.class);
    assertThat(map).containsEntry("key", "value");
    WIRE.verify(getRequestedFor(urlEqualTo(PATH_OK)));
  }

  @Test
  void wiremockTestNotOk() {
    Response response =
        DW.client().target("http://localhost:" + WIRE.getPort() + PATH_NOT_OK).request().get();

    assertThat(response.getStatus()).isEqualTo(404);
    WIRE.verify(getRequestedFor(urlEqualTo(PATH_NOT_OK)));
  }
}
