package org.sdase.commons.server.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsumerTokenBundleBuilderTest {

  @Test
  void shouldAddAutoExcludeForSwaggerAndOpenApi() {

    ConsumerTokenConfig config = new ConsumerTokenConfig();

    ConsumerTokenBundle<Configuration> bundle =
        ConsumerTokenBundle.builder().withConfigProvider(c -> config).build();

    bundle.run(
        Mockito.mock(Configuration.class),
        Mockito.mock(Environment.class, Mockito.RETURNS_DEEP_STUBS));

    assertThat(config.getExcludePatterns()).containsExactly("openapi\\.(json|yaml)");
  }
}
