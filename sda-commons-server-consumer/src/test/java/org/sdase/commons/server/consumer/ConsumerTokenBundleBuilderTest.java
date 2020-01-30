package org.sdase.commons.server.consumer;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ConsumerTokenBundleBuilderTest {

  @Test
  public void shouldAddAutoExcludeForSwagger() {

    ConsumerTokenConfig config = new ConsumerTokenConfig();

    ConsumerTokenBundle bundle =
        ConsumerTokenBundle.builder().withConfigProvider(c -> config).build();

    //noinspection unchecked
    bundle.run(
        Mockito.mock(Configuration.class),
        Mockito.mock(Environment.class, Mockito.RETURNS_DEEP_STUBS));

    Assertions.assertThat(config.getExcludePatterns()).containsExactly("swagger\\.(json|yaml)");
  }
}
