package org.sdase.commons.server.mongo.testing;

import static ch.qos.logback.classic.Level.WARN;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static de.flapdoodle.embed.mongo.distribution.Version.V5_0_14;
import static de.flapdoodle.embed.mongo.distribution.Version.V6_0_8;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.flapdoodle.embed.mongo.distribution.Version;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.RetryingTest;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;
import org.slf4j.LoggerFactory;

class StartLocalMongoDbTest {

  @RegisterExtension static WireMockClassExtension WIRE = new WireMockClassExtension();

  // These loggers produce extensive debug logs about the full download (including content).
  // That makes the download so slow that it times out.
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

  @Test
  @EnabledOnOs(OS.MAC)
  void shouldCreateWithFixedDownloadPathOnMac() {
    WIRE.stubFor(
        get(urlMatching(".*")).willReturn(aResponse().proxiedFrom("https://fastdl.mongodb.org")));

    runWithMongo(WIRE.baseUrl() + "/osx/mongodb-macos-x86_64-5.0.14.tgz", V5_0_14);

    WIRE.verify(getRequestedFor(urlPathEqualTo("/osx/mongodb-macos-x86_64-5.0.14.tgz")));
  }

  @RetryingTest(3)
  @EnabledOnOs(OS.LINUX)
  void shouldCreateWithFixedDownloadPathOnLinux() {
    WIRE.stubFor(
        get(urlMatching(".*")).willReturn(aResponse().proxiedFrom("https://fastdl.mongodb.org")));

    // match GH runner ubuntu-latest (currently 22.04), MongoDB 5 not available.
    // see https://github.com/actions/runner-images#available-images
    runWithMongo(WIRE.baseUrl() + "/linux/mongodb-linux-x86_64-ubuntu2204-6.0.8.tgz", V6_0_8);

    WIRE.verify(getRequestedFor(urlPathEqualTo("mongodb-linux-x86_64-ubuntu2204-6.0.8.tgz")));
  }

  void runWithMongo(String downloadPath, Version version) {
    if (downloadPath != null) {
      System.setProperty("EMBEDDED_MONGO_DOWNLOAD_PATH", downloadPath);
    }
    StartLocalMongoDb startLocalMongoDb =
        new StartLocalMongoDb("test", "test", "test", false, version, 1_000);
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
