package org.sdase.commons.server.weld.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.hamcrest.core.IsNull;
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
    assertThat(app, IsNull.notNullValue());
    assertThat(app.getFoo(), IsNull.notNullValue());
    assertThat(app.getFooEvent(), IsNull.notNullValue());
    assertThat(app.getSupplier(), IsNull.notNullValue());
    assertThat(app.getSupplier().get(), IsNull.notNullValue());
    assertThat(app.getSupplier().get(), IsNull.notNullValue());
    assertThat(app.getTestJobResult(), equalTo("foo"));
  }
}
