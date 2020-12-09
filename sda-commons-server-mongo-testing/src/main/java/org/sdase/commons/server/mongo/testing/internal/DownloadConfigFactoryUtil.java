package org.sdase.commons.server.mongo.testing.internal;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.config.store.HttpProxyFactory;
import de.flapdoodle.embed.process.config.store.ImmutableDownloadConfig;
import de.flapdoodle.embed.process.config.store.ProxyFactory;
import de.flapdoodle.embed.process.config.store.SameDownloadPathForEveryDistribution;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility that creates a {@link DownloadConfig} for the {@link
 * org.sdase.commons.server.mongo.testing.StartLocalMongoDbRule} depending on the environment
 * property {@value #PROXY_ENV_NAME}.
 *
 * <p>A fixed download URL may be configured by {@value #EMBEDDED_MONGO_DOWNLOAD_PATH_ENV_NAME}.
 */
public class DownloadConfigFactoryUtil {

  private static final String PROXY_ENV_NAME = "http_proxy";
  private static final String EMBEDDED_MONGO_DOWNLOAD_PATH_ENV_NAME =
      "EMBEDDED_MONGO_DOWNLOAD_PATH";

  private static final Logger LOG = LoggerFactory.getLogger(DownloadConfigFactoryUtil.class);

  private DownloadConfigFactoryUtil() {
    // utility
  }

  /** @return a download config that */
  public static DownloadConfig createDownloadConfig() {
    ImmutableDownloadConfig.Builder downloadConfigBuilder =
        Defaults.downloadConfigFor(Command.MongoD).proxyFactory(createProxyFactory());

    // Normally the mongod executable is downloaded directly from the
    // mongodb web page, however sometimes this behavior is undesired. Some
    // cases are proxy servers, missing internet access, or not wanting to
    // download executables from untrusted sources.
    //
    // Optional it is possible to download it from a source configured in
    // the environment variable:
    String embeddedMongoDownloadPath = System.getenv(EMBEDDED_MONGO_DOWNLOAD_PATH_ENV_NAME);

    if (embeddedMongoDownloadPath != null) {
      downloadConfigBuilder.downloadPath(
          new SameDownloadPathForEveryDistribution(embeddedMongoDownloadPath));
    }

    return downloadConfigBuilder.build();
  }

  private static Optional<ProxyFactory> createProxyFactory() {
    String httpProxy = System.getenv(PROXY_ENV_NAME);
    if (httpProxy != null) {
      try {
        URL url = new URL(httpProxy);

        if (url.getUserInfo() != null) {
          configureAuthentication(url);
        }

        return Optional.of(new HttpProxyFactory(url.getHost(), url.getPort()));
      } catch (MalformedURLException exception) {
        LOG.error("http_proxy could not be parsed.");
      }
    }
    return Optional.empty();
  }

  private static void configureAuthentication(URL url) {
    String userInfo = url.getUserInfo();
    int pos = userInfo.indexOf(':');
    if (pos >= 0) {
      String username = userInfo.substring(0, pos);
      String password = userInfo.substring(pos + 1);

      Authenticator.setDefault(
          new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              // Only provide the credentials to the specified
              // origin
              if (getRequestorType() == RequestorType.PROXY
                  && getRequestingHost().equalsIgnoreCase(url.getHost())
                  && url.getPort() == getRequestingPort()) {
                return new PasswordAuthentication(username, password.toCharArray());
              }
              return null;
            }
          });

      // Starting with Java 8u111, basic auth is not supported
      // for https by default.
      // jdk.http.auth.tunneling.disabledSchemes can be used to
      // enable it again.
      System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    } else {
      LOG.error("http_proxy user info could not be parsed.");
    }
  }
}
