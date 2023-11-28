package org.sdase.commons.server.mongo.testing;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static de.flapdoodle.embed.mongo.distribution.Version.V5_0_14;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.client.jersey.wiremock.testing.WireMockClassExtension;

class StartLocalMongoDbTest {

  @RegisterExtension static WireMockClassExtension WIRE = new WireMockClassExtension();

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

    runWithMongo(WIRE.baseUrl() + "/osx/mongodb-macos-x86_64-5.0.14.tgz");

    WIRE.verify(getRequestedFor(urlPathEqualTo("/osx/mongodb-macos-x86_64-5.0.14.tgz")));
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  void shouldCreateWithFixedDownloadPathOnLinux() {
    WIRE.stubFor(
        get(urlMatching(".*")).willReturn(aResponse().proxiedFrom("https://fastdl.mongodb.org")));

    runWithMongo(WIRE.baseUrl() + "/linux/mongodb-linux-x86_64-ubuntu2004-5.0.14.tgz");

    WIRE.verify(
        getRequestedFor(urlPathEqualTo("/linux/mongodb-linux-x86_64-ubuntu2004-5.0.14.tgz")));
  }

  void runWithMongo(String downloadPath) {
    if (downloadPath != null) {
      System.setProperty("EMBEDDED_MONGO_DOWNLOAD_PATH", downloadPath);
    }
    StartLocalMongoDb startLocalMongoDb =
        new StartLocalMongoDb("test", "test", "test", false, V5_0_14, 1_000);
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
