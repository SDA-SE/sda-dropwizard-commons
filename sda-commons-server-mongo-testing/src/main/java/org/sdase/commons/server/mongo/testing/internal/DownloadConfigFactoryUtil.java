package org.sdase.commons.server.mongo.testing.internal;

import de.flapdoodle.embed.mongo.packageresolver.Command;
import de.flapdoodle.embed.mongo.packageresolver.PlatformPackageResolver;
import de.flapdoodle.embed.process.config.DownloadConfig;
import de.flapdoodle.embed.process.config.store.ImmutablePackage;
import de.flapdoodle.embed.process.config.store.Package;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.distribution.Version;
import de.flapdoodle.embed.process.net.HttpProxyFactory;
import de.flapdoodle.embed.process.net.ProxyFactory;
import de.flapdoodle.embed.process.transitions.DownloadPackage;
import de.flapdoodle.os.CommonOS;
import de.flapdoodle.os.Platform;
import de.flapdoodle.reverse.Transition;
import de.flapdoodle.reverse.transitions.Start;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Optional;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility that creates a {@link DownloadConfig} for the {@link
 * org.sdase.commons.server.mongo.testing.StartLocalMongoDbClassExtension} depending on the
 * environment property {@value #PROXY_ENV_NAME}.
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

  public static Optional<Transition<Package>> createPackageOfDistribution(Version version) {
    // Normally the mongod executable is downloaded directly from the
    // mongodb web page, however sometimes this behavior is undesired. Some
    // cases are proxy servers, missing internet access, or not wanting to
    // download executables from untrusted sources.
    //
    // Optional it is possible to download it from a source configured in
    // the environment variable:
    String embeddedMongoDownloadPath =
        new SystemPropertyAndEnvironmentLookup().lookup(EMBEDDED_MONGO_DOWNLOAD_PATH_ENV_NAME);
    if (embeddedMongoDownloadPath == null) {
      return Optional.empty();
    } else {
      ImmutablePackage.Builder downloadPackage =
          Package.builder()
              .from(
                  new PlatformPackageResolver(Command.MongoD)
                      .packageFor(Distribution.of(version, Platform.detect(CommonOS.list()))));
      return Optional.of(
          Start.to(Package.class)
              .initializedWith(downloadPackage.url(embeddedMongoDownloadPath).build()));
    }
  }

  /**
   * @return a download config that
   */
  public static DownloadPackage createDownloadPackage() {
    return DownloadPackage.withDefaults()
        .withDownloadConfig(DownloadConfig.builder().proxyFactory(createProxyFactory()).build());
  }

  private static Optional<ProxyFactory> createProxyFactory() {
    String httpProxy = new SystemPropertyAndEnvironmentLookup().lookup(PROXY_ENV_NAME);
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
              // Only provide the credentials to the specified origin
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
