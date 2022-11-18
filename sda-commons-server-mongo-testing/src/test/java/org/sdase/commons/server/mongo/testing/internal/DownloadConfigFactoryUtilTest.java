package org.sdase.commons.server.mongo.testing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.mongo.testing.MongoDbRule;
import org.sdase.commons.server.testing.SystemPropertyRule;

class DownloadConfigFactoryUtilTest {

  @Test
  void shouldCreateWithFixedDownloadPath() throws Throwable {
    AtomicReference<DownloadConfig> actualConfig = new AtomicReference<>();
    new SystemPropertyRule()
        .setProperty("EMBEDDED_MONGO_DOWNLOAD_PATH", "http://localhost/mongo.tar.gz")
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                actualConfig.set(DownloadConfigFactoryUtil.createDownloadConfig());
              }
            },
            Description.EMPTY)
        .evaluate();
    assertThat(
            actualConfig
                .get()
                .getDownloadPath()
                .getPath(Distribution.detectFor(MongoDbRule.Builder.DEFAULT_VERSION)))
        .isEqualTo("http://localhost/mongo.tar.gz");
  }

  @Test
  void shouldCreateProxyWithAuthentication() throws Throwable {
    AtomicReference<DownloadConfig> actualConfig = new AtomicReference<>();
    new SystemPropertyRule()
        .setProperty("http_proxy", "http://tester:dummy@the-test-domain.example.com:1234")
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                actualConfig.set(DownloadConfigFactoryUtil.createDownloadConfig());
              }
            },
            Description.EMPTY)
        .evaluate();
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
