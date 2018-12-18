package org.sdase.commons.server.security.validation;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *    Checks that secure defaults of used {@link HttpConnectorFactory} instances are not modified and overwrites
 *    insecure defaults. This class checks for the risks identified in the security guide as:
 * </p>
 * <ul>
 *    <li>"Risiko: Verlust der der Quell-IP-Adresse"</li>
 *    <li>"Risiko: Erkennung von vertraulichen Komponenten ... Entfernen von applikations-bezogenen Headern"</li>
 * </ul>
 */
public class HttpConnectorSecurityAdvise {

   private static final Logger LOG = LoggerFactory.getLogger(HttpConnectorSecurityAdvise.class);

   private List<ConnectorFactory> connectorFactories;

   public HttpConnectorSecurityAdvise(ServerFactory serverFactory) {
      connectorFactories = new ArrayList<>();
      if (serverFactory instanceof DefaultServerFactory) {
         DefaultServerFactory defaultServerFactory = (DefaultServerFactory) serverFactory;
         connectorFactories.addAll(defaultServerFactory.getApplicationConnectors());
         connectorFactories.addAll(defaultServerFactory.getAdminConnectors());
      } else if (serverFactory instanceof SimpleServerFactory) {
         SimpleServerFactory simpleServerFactory = (SimpleServerFactory) serverFactory;
         connectorFactories.add(simpleServerFactory.getConnector());
      } else {
         LOG.error("Unable to apply secure connector config. Expecting a DefaultServerFactory or a SimpleServerFactory" +
                     " but found a {}", serverFactory.getClass());
      }
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
         LOG.warn("Unable to apply secure configuration to connector factory of type {}", connectorFactory.getClass());
      }
   }

   private void forceUseForwardedHeaders(HttpConnectorFactory httpConnectorFactory) {
      if (!httpConnectorFactory.isUseForwardedHeaders()) {
         throw new InsecureConfigurationException("Connector is not configured to use forwarded headers. " +
               "Check the configuration for useForwardedHeaders: false");
      }
   }

   private void forceNoServerHeader(HttpConnectorFactory httpConnectorFactory) {
      if (httpConnectorFactory.isUseServerHeader()) {
         throw new InsecureConfigurationException("Connector is configured to use server headers. " +
               "Check the configuration for useServerHeader: true");
      }
   }

   private void forceNoDateHeader(HttpConnectorFactory httpConnectorFactory) {
      if (httpConnectorFactory.isUseDateHeader()) {
         LOG.info("Disabling useDateHeader to avoid giving information to possible attackers.");
         httpConnectorFactory.setUseDateHeader(false);
      }
   }
}
