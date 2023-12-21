package org.sdase.commons.server.opa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.ws.rs.container.ContainerRequestContext;
import io.dropwizard.core.Configuration;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.opa.OpaBundle.DuplicatePropertyException;
import org.sdase.commons.server.opa.OpaBundle.HiddenOriginalPropertyException;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.extension.OpaInputExtension;
import org.sdase.commons.server.opa.extension.OpaInputHeadersExtension;

class OpaBundleTest {
  @Test
  void shouldThrowExceptionIfNamespaceCollidesWithOriginalProperty() {
    assertThatThrownBy(
            () ->
                OpaBundle.builder()
                    .withOpaConfigProvider(TestConfiguration::getOpa)
                    .withInputExtension("path", new NullInputExtension()))
        .isInstanceOf(HiddenOriginalPropertyException.class)
        .hasMessageContaining("NullInputExtension")
        .hasMessageContaining("\"path\"");
  }

  @Test
  void shouldThrowExceptionIfNamespaceCollidesWithOtherExtension() {
    assertThatThrownBy(
            () ->
                OpaBundle.builder()
                    .withOpaConfigProvider(TestConfiguration::getOpa)
                    .withInputExtension("myExtension", OpaInputHeadersExtension.builder().build())
                    .withInputExtension("myExtension", new NullInputExtension()))
        .isInstanceOf(DuplicatePropertyException.class)
        .hasMessageContaining("OpaInputHeadersExtension")
        .hasMessageContaining("NullInputExtension")
        .hasMessageContaining("\"myExtension\"");
  }

  @Test
  void shouldActivateHeadersExtensionByDefault() {
    OpaBundle<TestConfiguration> bundle =
        OpaBundle.builder().withOpaConfigProvider(TestConfiguration::getOpa).build();

    assertThat(bundle.getInputExtensions())
        .hasEntrySatisfying(
            "headers", ex -> assertThat(ex).isOfAnyClassIn(OpaInputHeadersExtension.class));
  }

  @Test
  void shouldNotActivateHeadersExtensionWhenDisabled() {
    OpaBundle<TestConfiguration> bundle =
        OpaBundle.builder()
            .withOpaConfigProvider(TestConfiguration::getOpa)
            .withoutHeadersExtension()
            .build();

    assertThat(bundle.getInputExtensions().values())
        .noneMatch(ex -> OpaInputHeadersExtension.class.equals(ex.getClass()));
  }

  static class NullInputExtension implements OpaInputExtension<Boolean> {
    @Override
    public Boolean createAdditionalInputContent(ContainerRequestContext requestContext) {
      return null;
    }
  }

  static class TestConfiguration extends Configuration {
    public OpaConfig getOpa() {
      return null;
    }
  }
}
