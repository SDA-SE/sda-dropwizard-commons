package org.sdase.commons.server.security.validation;

import io.dropwizard.core.server.AbstractServerFactory;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.server.ServerFactory;
import io.dropwizard.core.server.SimpleServerFactory;
import io.dropwizard.jetty.ConnectorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper utility that deals with {@link ServerFactory}s and their internals. */
class ServerFactoryUtil {

  private static final Logger LOG = LoggerFactory.getLogger(ServerFactoryUtil.class);

  private ServerFactoryUtil() {}

  /**
   * Extracts all {@link ConnectorFactory} instances from the given {@link ServerFactory}
   *
   * @param serverFactory the {@link ServerFactory} that should contain some {@link
   *     ConnectorFactory}s
   * @return the extracted {@link ConnectorFactory}s
   */
  static List<ConnectorFactory> extractConnectorFactories(ServerFactory serverFactory) {
    List<ConnectorFactory> connectorFactories = new ArrayList<>();
    if (serverFactory instanceof DefaultServerFactory defaultServerFactory) {
      connectorFactories.addAll(defaultServerFactory.getApplicationConnectors());
      connectorFactories.addAll(defaultServerFactory.getAdminConnectors());
    } else if (serverFactory instanceof SimpleServerFactory simpleServerFactory) {
      connectorFactories.add(simpleServerFactory.getConnector());
    } else if (serverFactory == null) {
      LOG.error(
          "Unable to apply secure connector config. Expecting a DefaultServerFactory or a SimpleServerFactory"
              + " but got null");
    } else {
      LOG.error(
          "Unable to apply secure connector config. Expecting a DefaultServerFactory or a SimpleServerFactory"
              + " but found a {}",
          serverFactory.getClass());
    }
    return connectorFactories;
  }

  /**
   * @param serverFactory the instance to check
   * @return the given {@code serverFactory} as {@link AbstractServerFactory} wrapped in an {@link
   *     Optional} or an empty {@link Optional} if the given {@code serverFactory} is not an {@link
   *     AbstractServerFactory}
   */
  static Optional<AbstractServerFactory> verifyAbstractServerFactory(ServerFactory serverFactory) {
    if (serverFactory instanceof AbstractServerFactory) {
      return Optional.of((AbstractServerFactory) serverFactory);
    } else if (serverFactory == null) {
      LOG.error(
          "Unable to apply secure server config. Expecting an AbstractServerFactory but found null");
    } else {
      LOG.error(
          "Unable to apply secure server config. Expecting an AbstractServerFactory but found a {}",
          serverFactory.getClass());
    }
    return Optional.empty();
  }
}
