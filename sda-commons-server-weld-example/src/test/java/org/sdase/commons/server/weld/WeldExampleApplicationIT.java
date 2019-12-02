package org.sdase.commons.server.weld;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.weld.testing.WeldAppRule;

public class WeldExampleApplicationIT {

   @ClassRule
   public static final DropwizardAppRule<Configuration> RULE = new WeldAppRule<>(WeldExampleApplication.class,
         ResourceHelpers.resourceFilePath("test-config.yaml"));

   @Test
   public void shouldBeInjectedCorrectly() {
      WeldExampleApplication app = RULE.getApplication();
      Assertions.assertThat(app.getUsageBean()).isNotNull();
   }

}
