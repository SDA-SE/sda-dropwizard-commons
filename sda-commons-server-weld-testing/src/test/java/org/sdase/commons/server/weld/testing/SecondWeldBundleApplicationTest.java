package org.sdase.commons.server.weld.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.weld.testing.test.AppConfiguration;
import org.sdase.commons.server.weld.testing.test.WeldExampleApplication;

public class SecondWeldBundleApplicationTest {

  @ClassRule
  public static final DropwizardAppRule<AppConfiguration> RULE =
      new WeldAppRule<>(WeldExampleApplication.class, resourceFilePath("test-config.yaml"));

  @Test
  public void testInjectedObjects() {
    WeldExampleApplication app = RULE.getApplication();
    assertThat(app).isNotNull();
    assertThat(app.getFoo()).isNotNull();
    assertThat(app.getFooEvent()).isNotNull();
    assertThat(app.getSupplier()).isNotNull();
    assertThat(app.getSupplier().get()).isNotNull();
    assertThat(app.getSupplier().get()).isNotNull();
    assertThat(app.getTestJobResult()).isEqualTo("foo");
  }
}
