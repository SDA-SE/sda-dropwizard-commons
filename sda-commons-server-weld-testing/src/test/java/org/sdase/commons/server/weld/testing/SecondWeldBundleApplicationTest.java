package org.sdase.commons.server.weld.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.weld.testing.test.AppConfiguration;
import org.sdase.commons.server.weld.testing.test.WeldExampleApplication;

class SecondWeldBundleApplicationTest {

  @RegisterExtension
  static final WeldAppExtension<AppConfiguration> APP =
      new WeldAppExtension<>(WeldExampleApplication.class, resourceFilePath("test-config.yaml"));

  @Test
  void testInjectedObjects() {
    WeldExampleApplication app = APP.getApplication();
    assertThat(app).isNotNull();
    assertThat(app.getFoo()).isNotNull();
    assertThat(app.getFooEvent()).isNotNull();
    assertThat(app.getSupplier()).isNotNull();
    assertThat(app.getSupplier().get()).isNotNull();
    assertThat(app.getSupplier().get()).isNotNull();
    assertThat(app.getTestJobResult()).isEqualTo("foo");
  }
}
