package org.sdase.commons.client.jersey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.Test;

class JerseyClientBundleTest {

  @Test
  void doNotAccessClientBuilderBeforeInitialize() {

    JerseyClientBundle<Configuration> clientBundle =
        JerseyClientBundle.builder().withConsumerTokenProvider(c -> "test").build();

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(clientBundle::getClientFactory);
  }

  @Test
  void doNotAccessClientBuilderBeforeRun() {

    JerseyClientBundle<Configuration> clientBundle =
        JerseyClientBundle.builder().withConsumerTokenProvider(c -> "test").build();

    clientBundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(clientBundle::getClientFactory);
  }

  @Test
  void accessClientBuilderAfterRun() {

    JerseyClientBundle<Configuration> clientBundle =
        JerseyClientBundle.builder().withConsumerTokenProvider(c -> "test").build();

    clientBundle.initialize(mock(Bootstrap.class, RETURNS_DEEP_STUBS));
    clientBundle.run(new Configuration(), mock(Environment.class, RETURNS_DEEP_STUBS));

    assertThat(clientBundle.getClientFactory()).isNotNull();
  }
}
