package org.sdase.commons.client.jersey.wiremock.testing;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Junit 5 replacement for {@link com.github.tomakehurst.wiremock.junit.WireMockClassRule} */
public class WireMockClassExtension extends WireMockServer
    implements BeforeAllCallback, AfterAllCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(WireMockClassExtension.class);

  /** Constructor that creates a new instance with dynamic port */
  public WireMockClassExtension() {
    this(wireMockConfig().dynamicPort());
  }

  public WireMockClassExtension(Options options) {
    super(options);
  }

  public WireMockClassExtension(int port, Integer httpsPort) {
    this(wireMockConfig().port(port).httpsPort(httpsPort));
  }

  public WireMockClassExtension(int port) {
    this(wireMockConfig().port(port));
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    LOGGER.info("Start WireMock");
    start();
    if (options.getHttpDisabled()) {
      WireMock.configureFor("https", "localhost", httpsPort());
    } else {
      WireMock.configureFor("http", "localhost", port());
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    LOGGER.info("Stop WireMock");
    stop();
  }
}
