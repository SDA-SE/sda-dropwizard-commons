package org.sdase.commons.server.security.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sdase.commons.server.security.validation.ServerFactoryUtil.verifyAbstractServerFactory;

import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ServerFactoryUtilTest {

  @Test
  void isAbstractServerFactory() {
    ServerFactory serverFactory = mock(AbstractServerFactory.class);
    Optional<AbstractServerFactory> actual = verifyAbstractServerFactory(serverFactory);
    assertThat(actual).isPresent();
  }

  @Test
  void serverFactoryIsNotAbstractServerFactory() {
    ServerFactory serverFactory = mock(ServerFactory.class);
    Optional<AbstractServerFactory> actual = verifyAbstractServerFactory(serverFactory);
    assertThat(actual).isNotPresent();
  }

  @Test
  void nullIsNotAbstractServerFactory() {
    Optional<AbstractServerFactory> actual = verifyAbstractServerFactory(null);
    assertThat(actual).isNotPresent();
  }

  @Test
  void extractNoConnectorsFromNull() {
    assertThat(ServerFactoryUtil.extractConnectorFactories(null)).isEmpty();
  }

  @Test
  void extractNoConnectorsFromUnknownServerFactoryType() {
    assertThat(ServerFactoryUtil.extractConnectorFactories(mock(ServerFactory.class))).isEmpty();
  }

  @Test
  void extractConnectorsFromSimpleServerFactory() {
    ConnectorFactory connectorFactoryMock = mock(ConnectorFactory.class);
    SimpleServerFactory simpleServerFactory = mock(SimpleServerFactory.class);
    when(simpleServerFactory.getConnector()).thenReturn(connectorFactoryMock);

    List<ConnectorFactory> actual =
        ServerFactoryUtil.extractConnectorFactories(simpleServerFactory);

    assertThat(actual).containsExactly(connectorFactoryMock);
  }

  @Test
  void extractConnectorsFromDefaultServerFactory() {
    ConnectorFactory firstAppConnector = mock(ConnectorFactory.class);
    ConnectorFactory secondAppConnector = mock(ConnectorFactory.class);
    ConnectorFactory adminConnector = mock(ConnectorFactory.class);
    DefaultServerFactory serverFactory = mock(DefaultServerFactory.class);
    when(serverFactory.getApplicationConnectors())
        .thenReturn(asList(firstAppConnector, secondAppConnector));
    when(serverFactory.getAdminConnectors()).thenReturn(singletonList(adminConnector));

    List<ConnectorFactory> actual = ServerFactoryUtil.extractConnectorFactories(serverFactory);

    assertThat(actual).containsExactly(firstAppConnector, secondAppConnector, adminConnector);
  }
}
