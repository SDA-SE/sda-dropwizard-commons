package org.sdase.commons.server.mongo.testing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.mongo.testing.MongoDbClassExtension;

class DownloadConfigFactoryUtilTest {

  @Test
  @SetSystemProperty(key = "EMBEDDED_MONGO_DOWNLOAD_PATH", value = "http://localhost/mongo.tar.gz")
  void shouldCreateWithFixedDownloadPath() {
    AtomicReference<DownloadConfig> actualConfig = new AtomicReference<>();
    actualConfig.set(DownloadConfigFactoryUtil.createDownloadConfig());
    assertThat(
            actualConfig
                .get()
                .getDownloadPath()
                .getPath(Distribution.detectFor(MongoDbClassExtension.Builder.DEFAULT_VERSION)))
        .isEqualTo("http://localhost/mongo.tar.gz");
  }

  @Test
  @SetSystemProperty(
      key = "http_proxy",
      value = "http://tester:dummy@the-test-domain.example.com:1234")
  void shouldCreateProxyWithAuthentication() throws Throwable {
    AtomicReference<DownloadConfig> actualConfig = new AtomicReference<>();
    actualConfig.set(DownloadConfigFactoryUtil.createDownloadConfig());
    assertThat(actualConfig.get()).isNotNull();
    PasswordAuthentication authentication =
        Authenticator.requestPasswordAuthentication(
            "the-test-domain.example.com",
            null,
            1234,
            "http",
            "none",
            "http",
            new URL("http://tester:dummy@the-test-domain.example.com:1234"),
            Authenticator.RequestorType.PROXY);
    assertThat(authentication.getUserName()).isEqualTo("tester");
    assertThat(authentication.getPassword()).isEqualTo("dummy".toCharArray());
  }
}
