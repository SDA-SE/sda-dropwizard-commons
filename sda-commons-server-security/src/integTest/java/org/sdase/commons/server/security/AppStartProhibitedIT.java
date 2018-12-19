package org.sdase.commons.server.security;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.sdase.commons.server.security.test.SecurityTestApp;
import org.sdase.commons.server.testing.EnvironmentRule;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.runner.Description.createTestDescription;

/**
 * Tests that the app is not started with insecure configuration.
 */
@RunWith(Parameterized.class)
public class AppStartProhibitedIT {

   private EnvironmentRule env;
   private Class<? extends Throwable> expectedException;

   public AppStartProhibitedIT(String givenEnvKey, String givenEnvValue, Class<? extends Throwable> expectedException) {
      this.env = new EnvironmentRule().setEnv(givenEnvKey, givenEnvValue);
      this.expectedException = expectedException;
   }


   @Parameters(name = "{0}: {2}")
   public static Collection<Object[]> data() {
      return Arrays.asList(
            // verify good default not modified
            new Object[] {"ALLOWED_METHODS", "[\"GET\", \"TRACE\"]", InsecureConfigurationException.class},
            new Object[] {"START_AS_ROOT", "true", InsecureConfigurationException.class},
            new Object[] {"USE_FORWARDED_HEADERS_APP", "false", InsecureConfigurationException.class},
            new Object[] {"USE_FORWARDED_HEADERS_ADMIN", "false", InsecureConfigurationException.class},
            new Object[] {"USE_SERVER_HEADER_APP", "true", InsecureConfigurationException.class},
            new Object[] {"USE_SERVER_HEADER_ADMIN", "true", InsecureConfigurationException.class},
            new Object[] {"DISABLE_JACKSON_CONFIGURATION", "true", InsecureConfigurationException.class},
            // auto reconfigured because true is the default
            new Object[] {"USE_DATE_HEADER_APP", "true", null},
            new Object[] {"USE_DATE_HEADER_ADMIN", "true", null},
            new Object[] {"REGISTER_DEFAULT_EXCEPTION_MAPPERS", "true", null},
            // counter check startup with secure config
            new Object[] {"default", "config", null} // NOSONAR
      );
   }

   @Test
   public void shouldStartOnlyIfExpectedExceptionIsNull() throws Throwable {
      Statement appStarter = createAppStarter();
      if (expectedException == null) {
         appStarter.evaluate(); // throws exception if app can not start
      }
      else {
         assertThatExceptionOfType(expectedException).isThrownBy(appStarter::evaluate);
      }
   }

   private Statement createAppStarter() {
      DropwizardAppRule<Configuration> dwRule = new DropwizardAppRule<>(
            SecurityTestApp.class,
            ResourceHelpers.resourceFilePath("test-config-insecure-settings.yaml")
      );
      return RuleChain.outerRule(env).around(dwRule).apply(
            new Statement() {
               @Override
               public void evaluate() {
                  assertThat(dwRule.getTestSupport().getConfiguration()).isNotNull();
               }
            },
            createTestDescription(AppStartProhibitedIT.class, expectedException == null ? "shouldStart" : "shouldNotStart")
      );
   }
}
