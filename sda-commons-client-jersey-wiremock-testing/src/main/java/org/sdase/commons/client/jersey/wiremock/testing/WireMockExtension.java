package org.sdase.commons.client.jersey.wiremock.testing;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Junit 5 replacement for {@link com.github.tomakehurst.wiremock.junit.WireMockRule} */
public class WireMockExtension extends WireMockServer
    implements BeforeEachCallback, AfterEachCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(WireMockExtension.class);

  private final boolean failOnUnmatchedRequests;

  public WireMockExtension(Options options) {
    this(options, true);
  }

  public WireMockExtension(Options options, boolean failOnUnmatchedRequests) {
    super(options);
    this.failOnUnmatchedRequests = failOnUnmatchedRequests;
  }

  public WireMockExtension(int port) {
    this(wireMockConfig().port(port));
  }

  public WireMockExtension(int port, Integer httpsPort) {
    this(wireMockConfig().port(port).httpsPort(httpsPort));
  }

  public WireMockExtension() {
    this(wireMockConfig());
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    LOGGER.info("Start WireMock");
    start();

    if (options.getHttpDisabled()) {
      WireMock.configureFor("https", "localhost", httpsPort());
    } else {
      WireMock.configureFor("localhost", port());
    }
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    try {
      checkForUnmatchedRequests();
    } finally {
      LOGGER.info("Stop WireMock");
      stop();
    }
  }

  private void checkForUnmatchedRequests() {
    if (failOnUnmatchedRequests) {
      List<LoggedRequest> unmatchedRequests = findAllUnmatchedRequests();
      if (!unmatchedRequests.isEmpty()) {
        List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
        if (nearMisses.isEmpty()) {
          throw VerificationException.forUnmatchedRequests(unmatchedRequests);
        } else {
          throw VerificationException.forUnmatchedNearMisses(nearMisses);
        }
      }
    }
  }
}
