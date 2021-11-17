package org.sdase.commons.server.security;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.runner.Description.createTestDescription;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.Statement;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.sdase.commons.server.security.test.SecurityTestApp;
import org.sdase.commons.server.testing.SystemPropertyRule;

/** Tests that the app is not started with insecure configuration. */
@RunWith(Parameterized.class)
public class AppStartProhibitedIT {

  private SystemPropertyRule env;
  private Class<? extends Throwable> expectedException;

  public AppStartProhibitedIT(
      String givenEnvKey,
      String givenEnvValue,
      Class<? extends Throwable> expectedException,
      Map<String, String> additionalSystemProperties) {
    this.env = new SystemPropertyRule().setProperty(givenEnvKey, givenEnvValue);
    if (additionalSystemProperties != null) {
      additionalSystemProperties.forEach(env::setProperty);
    }
    this.expectedException = expectedException;
  }

  @Parameters(name = "{0}: {2}")
  public static Collection<Object[]> data() {
    String disableBufferCheckKey = "DISABLE_BUFFER_CHECK";
    return Arrays.asList(
        // verify good default not modified
        new Object[] {
          "ALLOWED_METHODS", "[\"GET\", \"TRACE\"]", InsecureConfigurationException.class, null
        },
        new Object[] {"START_AS_ROOT", "true", InsecureConfigurationException.class, null},
        new Object[] {"USE_SERVER_HEADER_APP", "true", InsecureConfigurationException.class, null},
        new Object[] {
          "USE_SERVER_HEADER_ADMIN", "true", InsecureConfigurationException.class, null
        },
        new Object[] {
          "DISABLE_JACKSON_CONFIGURATION", "true", InsecureConfigurationException.class, null
        },
        new Object[] {
          "HEADER_CACHE_SIZE_APP", "513 bytes", InsecureConfigurationException.class, null
        }, // NOSONAR
        new Object[] {
          "HEADER_CACHE_SIZE_ADMIN", "513 bytes", InsecureConfigurationException.class, null
        },
        new Object[] {
          "OUTPUT_BUFFER_SIZE_APP", "33KiB", InsecureConfigurationException.class, null
        }, // NOSONAR
        new Object[] {
          "OUTPUT_BUFFER_SIZE_ADMIN", "33KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "MAX_REQUEST_HEADER_SIZE_APP", "9KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "MAX_REQUEST_HEADER_SIZE_ADMIN", "9KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "MAX_RESPONSE_HEADER_SIZE_APP", "9KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "MAX_RESPONSE_HEADER_SIZE_ADMIN", "9KiB", InsecureConfigurationException.class, null
        },
        new Object[] {"INPUT_BUFFER_SIZE_APP", "9KiB", InsecureConfigurationException.class, null},
        new Object[] {
          "INPUT_BUFFER_SIZE_ADMIN", "9KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "MIN_BUFFER_POOL_SIZE_APP", "65 bytes", InsecureConfigurationException.class, null
        }, // NOSONAR
        new Object[] {
          "MIN_BUFFER_POOL_SIZE_ADMIN", "65 bytes", InsecureConfigurationException.class, null
        },
        new Object[] {
          "BUFFER_POOL_INCREMENT_APP", "2KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "BUFFER_POOL_INCREMENT_ADMIN", "2KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "MAX_BUFFER_POOL_SIZE_APP", "65KiB", InsecureConfigurationException.class, null
        }, // NOSONAR
        new Object[] {
          "MAX_BUFFER_POOL_SIZE_ADMIN", "65KiB", InsecureConfigurationException.class, null
        },
        new Object[] {
          "HEADER_CACHE_SIZE_APP", "513 bytes", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "HEADER_CACHE_SIZE_ADMIN", "513 bytes", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "OUTPUT_BUFFER_SIZE_APP", "33KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "OUTPUT_BUFFER_SIZE_ADMIN", "33KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MAX_REQUEST_HEADER_SIZE_APP", "9KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MAX_REQUEST_HEADER_SIZE_ADMIN", "9KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MAX_RESPONSE_HEADER_SIZE_APP", "9KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MAX_RESPONSE_HEADER_SIZE_ADMIN",
          "9KiB",
          null,
          singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "INPUT_BUFFER_SIZE_APP", "9KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "INPUT_BUFFER_SIZE_ADMIN", "9KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MIN_BUFFER_POOL_SIZE_APP", "65 bytes", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MIN_BUFFER_POOL_SIZE_ADMIN",
          "65 bytes",
          null,
          singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "BUFFER_POOL_INCREMENT_APP", "2KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "BUFFER_POOL_INCREMENT_ADMIN", "2KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MAX_BUFFER_POOL_SIZE_APP", "65KiB", null, singletonMap(disableBufferCheckKey, "true")
        },
        new Object[] {
          "MAX_BUFFER_POOL_SIZE_ADMIN", "65KiB", null, singletonMap(disableBufferCheckKey, "true")
        },

        // auto reconfigured because true is the default
        new Object[] {"USE_DATE_HEADER_APP", "true", null, null},
        new Object[] {"USE_DATE_HEADER_ADMIN", "true", null, null},
        new Object[] {"REGISTER_DEFAULT_EXCEPTION_MAPPERS", "true", null, null},
        // counter check startup with secure config
        new Object[] {"default", "config", null, null} // NOSONAR
        );
  }

  @Test
  public void shouldStartOnlyIfExpectedExceptionIsNull() throws Throwable {
    Statement appStarter = createAppStarter();
    if (expectedException == null) {
      appStarter.evaluate(); // throws exception if app can not start
    } else {
      assertThatExceptionOfType(expectedException).isThrownBy(appStarter::evaluate);
    }
  }

  private Statement createAppStarter() {
    DropwizardAppRule<Configuration> dwRule =
        new DropwizardAppRule<>(
            SecurityTestApp.class,
            ResourceHelpers.resourceFilePath("test-config-insecure-settings.yaml"));
    return RuleChain.outerRule(env)
        .around(dwRule)
        .apply(
            new Statement() {
              @Override
              public void evaluate() {
                assertThat(dwRule.getTestSupport().getConfiguration()).isNotNull();
              }
            },
            createTestDescription(
                AppStartProhibitedIT.class,
                expectedException == null ? "shouldStart" : "shouldNotStart"));
  }
}
