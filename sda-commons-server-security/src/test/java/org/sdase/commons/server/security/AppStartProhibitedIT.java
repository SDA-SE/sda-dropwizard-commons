package org.sdase.commons.server.security;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.params.provider.Arguments.of;

import io.dropwizard.Configuration;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sdase.commons.server.security.exception.InsecureConfigurationException;
import org.sdase.commons.server.security.test.SecurityTestApp;
import org.sdase.commons.server.testing.SystemPropertyClassExtension;

/** Tests that the app is not started with insecure configuration. */
class AppStartProhibitedIT {

  // must be handled manually in this test
  SystemPropertyClassExtension testProperties = new SystemPropertyClassExtension();

  static Stream<Arguments> data() {
    return Stream.of(
        // verify good default not modified
        of("ALLOWED_METHODS", "[\"GET\", \"TRACE\"]", InsecureConfigurationException.class, null),
        of("START_AS_ROOT", "true", InsecureConfigurationException.class, null),
        of("USE_SERVER_HEADER_APP", "true", InsecureConfigurationException.class, null),
        of("USE_SERVER_HEADER_ADMIN", "true", InsecureConfigurationException.class, null),
        of("DISABLE_JACKSON_CONFIGURATION", "true", InsecureConfigurationException.class, null),
        of("HEADER_CACHE_SIZE_APP", "513 bytes", InsecureConfigurationException.class, null),
        of("HEADER_CACHE_SIZE_ADMIN", "513 bytes", InsecureConfigurationException.class, null),
        of("OUTPUT_BUFFER_SIZE_APP", "33KiB", InsecureConfigurationException.class, null),
        of("OUTPUT_BUFFER_SIZE_ADMIN", "33KiB", InsecureConfigurationException.class, null),
        of("MAX_REQUEST_HEADER_SIZE_APP", "9KiB", InsecureConfigurationException.class, null),
        of("MAX_REQUEST_HEADER_SIZE_ADMIN", "9KiB", InsecureConfigurationException.class, null),
        of("MAX_RESPONSE_HEADER_SIZE_APP", "9KiB", InsecureConfigurationException.class, null),
        of("MAX_RESPONSE_HEADER_SIZE_ADMIN", "9KiB", InsecureConfigurationException.class, null),
        of("INPUT_BUFFER_SIZE_APP", "9KiB", InsecureConfigurationException.class, null),
        of("INPUT_BUFFER_SIZE_ADMIN", "9KiB", InsecureConfigurationException.class, null),
        of("MIN_BUFFER_POOL_SIZE_APP", "65 bytes", InsecureConfigurationException.class, null),
        of("MIN_BUFFER_POOL_SIZE_ADMIN", "65 bytes", InsecureConfigurationException.class, null),
        of("BUFFER_POOL_INCREMENT_APP", "2KiB", InsecureConfigurationException.class, null),
        of("BUFFER_POOL_INCREMENT_ADMIN", "2KiB", InsecureConfigurationException.class, null),
        of("MAX_BUFFER_POOL_SIZE_APP", "65KiB", InsecureConfigurationException.class, null),
        of("MAX_BUFFER_POOL_SIZE_ADMIN", "65KiB", InsecureConfigurationException.class, null),
        of("HEADER_CACHE_SIZE_APP", "513 bytes", null, disableBufferCheck()),
        of("HEADER_CACHE_SIZE_ADMIN", "513 bytes", null, disableBufferCheck()),
        of("OUTPUT_BUFFER_SIZE_APP", "33KiB", null, disableBufferCheck()),
        of("OUTPUT_BUFFER_SIZE_ADMIN", "33KiB", null, disableBufferCheck()),
        of("MAX_REQUEST_HEADER_SIZE_APP", "9KiB", null, disableBufferCheck()),
        of("MAX_REQUEST_HEADER_SIZE_ADMIN", "9KiB", null, disableBufferCheck()),
        of("MAX_RESPONSE_HEADER_SIZE_APP", "9KiB", null, disableBufferCheck()),
        of("MAX_RESPONSE_HEADER_SIZE_ADMIN", "9KiB", null, disableBufferCheck()),
        of("INPUT_BUFFER_SIZE_APP", "9KiB", null, disableBufferCheck()),
        of("INPUT_BUFFER_SIZE_ADMIN", "9KiB", null, disableBufferCheck()),
        of("MIN_BUFFER_POOL_SIZE_APP", "65 bytes", null, disableBufferCheck()),
        of("MIN_BUFFER_POOL_SIZE_ADMIN", "65 bytes", null, disableBufferCheck()),
        of("BUFFER_POOL_INCREMENT_APP", "2KiB", null, disableBufferCheck()),
        of("BUFFER_POOL_INCREMENT_ADMIN", "2KiB", null, disableBufferCheck()),
        of("MAX_BUFFER_POOL_SIZE_APP", "65KiB", null, disableBufferCheck()),
        of("MAX_BUFFER_POOL_SIZE_ADMIN", "65KiB", null, disableBufferCheck()),

        // auto reconfigured because true is the default
        of("USE_DATE_HEADER_APP", "true", null, null),
        of("USE_DATE_HEADER_ADMIN", "true", null, null),
        of("REGISTER_DEFAULT_EXCEPTION_MAPPERS", "true", null, null),
        // counter check startup with secure config
        of("default", "config", null, null));
  }

  @ParameterizedTest
  @MethodSource("data")
  void shouldStartOnlyIfExpectedExceptionIsNull(
      String givenEnvKey,
      String givenEnvValue,
      Class<? extends Throwable> expectedException,
      Map<String, String> additionalSystemProperties)
      throws Throwable {

    var givenApp = createTestApp(givenEnvKey, givenEnvValue, additionalSystemProperties);

    try {
      if (expectedException == null) {
        assertThatNoException().isThrownBy(givenApp::before);
        assertThat(givenApp.getConfiguration()).isNotNull();
      } else {
        assertThatExceptionOfType(expectedException).isThrownBy(givenApp::before);
      }

    } finally {
      givenApp.after();
      testProperties.afterAll(null);
    }
  }

  private DropwizardTestSupport<Configuration> createTestApp(
      String givenEnvKey, String givenEnvValue, Map<String, String> additionalSystemProperties)
      throws Exception {
    setGivenTestProperties(givenEnvKey, givenEnvValue, additionalSystemProperties);
    return new DropwizardTestSupport<>(
        SecurityTestApp.class,
        ResourceHelpers.resourceFilePath("test-config-insecure-settings.yaml"));
  }

  private void setGivenTestProperties(
      String givenEnvKey, String givenEnvValue, Map<String, String> additionalSystemProperties)
      throws Exception {
    testProperties.setProperty(givenEnvKey, givenEnvValue);
    if (additionalSystemProperties != null) {
      additionalSystemProperties.forEach(testProperties::setProperty);
    }
    testProperties.beforeAll(null);
  }

  private static Map<String, String> disableBufferCheck() {
    return singletonMap("DISABLE_BUFFER_CHECK", "true");
  }
}
