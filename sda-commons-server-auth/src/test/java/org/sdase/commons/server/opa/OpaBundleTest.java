package org.sdase.commons.server.opa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dropwizard.Configuration;
import javax.ws.rs.container.ContainerRequestContext;
import org.junit.Test;
import org.sdase.commons.server.opa.OpaBundle.DuplicatePropertyException;
import org.sdase.commons.server.opa.OpaBundle.HiddenOriginalPropertyException;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.extension.OpaInputExtension;
import org.sdase.commons.server.opa.extension.OpaInputHeadersExtension;

public class OpaBundleTest {
  @Test
  public void shouldThrowExceptionIfNamespaceCollidesWithOriginalProperty() {
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
  public void shouldThrowExceptionIfNamespaceCollidesWithOtherExtension() {
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
  public void shouldActivateHeadersExtensionByDefault() {
    OpaBundle<TestConfiguration> bundle =
        OpaBundle.builder().withOpaConfigProvider(TestConfiguration::getOpa).build();

    assertThat(bundle.getInputExtensions())
        .hasEntrySatisfying(
            "headers", ex -> assertThat(ex).isOfAnyClassIn(OpaInputHeadersExtension.class));
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
