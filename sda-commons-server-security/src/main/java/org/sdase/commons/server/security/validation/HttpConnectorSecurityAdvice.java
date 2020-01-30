package org.sdase.commons.server.security.validation;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.ServerFactory;
import java.util.List;
import java.util.Objects;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that secure defaults of used {@link HttpConnectorFactory} instances are not modified and
 * overwrites insecure defaults. This class checks for the risks identified in the security guide
 * as:
 *
 * <ul>
 *   <li>"Risiko: Verlust der der Quell-IP-Adresse"
 *   <li>"Risiko: Erkennung von vertraulichen Komponenten ... Entfernen von applikations-bezogenen
 *       Headern"
 * </ul>
 */
public class HttpConnectorSecurityAdvice {

  private static final Logger LOG = LoggerFactory.getLogger(HttpConnectorSecurityAdvice.class);

  private List<ConnectorFactory> connectorFactories;

  public HttpConnectorSecurityAdvice(ServerFactory serverFactory) {
    this.connectorFactories = ServerFactoryUtil.extractConnectorFactories(serverFactory);
  }

  public void applySecureConfiguration() {
    connectorFactories.stream().filter(Objects::nonNull).forEach(this::applySecureConfiguration);
  }

  private void applySecureConfiguration(ConnectorFactory connectorFactory) {
    if (connectorFactory instanceof HttpConnectorFactory) {
      HttpConnectorFactory httpConnectorFactory = (HttpConnectorFactory) connectorFactory;
      forceUseForwardedHeaders(httpConnectorFactory);
      forceNoServerHeader(httpConnectorFactory);
      forceNoDateHeader(httpConnectorFactory);
    } else {
      LOG.warn(
          "Unable to apply secure configuration to connector factory of type {}",
          connectorFactory.getClass());
    }
  }

  private void forceUseForwardedHeaders(HttpConnectorFactory httpConnectorFactory) {
    if (!httpConnectorFactory.isUseForwardedHeaders()) {
      throw new InsecureConfigurationException(
          "Connector is not configured to use forwarded headers. "
              + "Check the configuration for useForwardedHeaders: false");
    }
  }

  private void forceNoServerHeader(HttpConnectorFactory httpConnectorFactory) {
    if (httpConnectorFactory.isUseServerHeader()) {
      throw new InsecureConfigurationException(
          "Connector is configured to use server headers. "
              + "Check the configuration for useServerHeader: true");
    }
  }

  private void forceNoDateHeader(HttpConnectorFactory httpConnectorFactory) {
    if (httpConnectorFactory.isUseDateHeader()) {
      LOG.debug("Disabling useDateHeader to avoid giving information to possible attackers.");
      httpConnectorFactory.setUseDateHeader(false);
    }
  }
}
