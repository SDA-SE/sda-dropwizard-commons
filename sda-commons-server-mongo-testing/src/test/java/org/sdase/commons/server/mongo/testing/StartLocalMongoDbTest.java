package org.sdase.commons.server.mongo.testing;

import static ch.qos.logback.classic.Level.WARN;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static de.flapdoodle.embed.mongo.distribution.Version.Main.V7_0;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.mongo.packageresolver.PlatformPackageResolver;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.Platform;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.slf4j.LoggerFactory;

class StartLocalMongoDbTest {

  @RegisterExtension static WireMockClassExtension WIRE = new WireMockClassExtension();

  static final IFeatureAwareVersion TEST_VERSION = V7_0;

  // These loggers produce extensive debug logs about the full download (including content).
  // That makes the download so slow that it times out and produces very large build artifacts.
  static final Map<ch.qos.logback.classic.Logger, Level> reconfiguredLoggersAndOriginalLevel =
      Stream.of(
              "org.eclipse.jetty",
              "org.apache.hc.client5.http.wire",
              "org.apache.hc.client5.http.headers",
              "org.apache.hc.client5.http")
          .map(LoggerFactory::getLogger)
          .filter(ch.qos.logback.classic.Logger.class::isInstance)
          .map(ch.qos.logback.classic.Logger.class::cast)
          .collect(
              Collectors.toMap(
                  Function.identity(),
                  l -> {
                    Level effectiveLevel = l.getEffectiveLevel();
                    l.setLevel(WARN);
                    return effectiveLevel;
                  }));

  @AfterAll
  static void resetLogger() {
    reconfiguredLoggersAndOriginalLevel.forEach(Logger::setLevel);
  }

  @BeforeEach
  @AfterEach
  void resetStubs() {
    WIRE.resetAll();
  }

  @RetryingTest(5)
  void shouldCreateWithFixedDownloadPath() {
    String path =
        new PlatformPackageResolver(Command.MongoD)
            .packageFor(Distribution.of(TEST_VERSION, Platform.detect(CommonOS.list())))
            .url();
    WIRE.stubFor(
        get(urlMatching(".*")).willReturn(aResponse().proxiedFrom("https://fastdl.mongodb.org")));
    try {
      runWithMongo(WIRE.baseUrl() + path);
    } catch (Throwable t) {
      // We accept that the downloaded MongoDB is not starting.
      // It is assumed, that the wrong package is selected.
      // Here we only want to test that another location is used for the download.
      // We have other tests using flapdoodle which show that it works in general.
      assertThat(t)
          .hasStackTraceContaining(
              "rollback after error on transition to State(RunningMongodProcess)");
    }

    WIRE.verify(getRequestedFor(urlPathEqualTo(path)));
  }

  void runWithMongo(String downloadPath) {
    if (downloadPath != null) {
      System.setProperty("EMBEDDED_MONGO_DOWNLOAD_PATH", downloadPath);
    }
    StartLocalMongoDb startLocalMongoDb =
        new StartLocalMongoDb("test", "test", "test", false, TEST_VERSION, 1_000);
    try {
      startLocalMongoDb.startMongo();
    } finally {
      if (downloadPath != null) {
        System.clearProperty("EMBEDDED_MONGO_DOWNLOAD_PATH");
      }
      startLocalMongoDb.stopMongo();
    }
  }
}
