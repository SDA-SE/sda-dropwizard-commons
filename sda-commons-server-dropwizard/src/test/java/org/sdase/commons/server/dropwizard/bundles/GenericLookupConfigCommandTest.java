package org.sdase.commons.server.dropwizard.bundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.dropwizard.testing.DropwizardTestSupport;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

class GenericLookupConfigCommandTest {

  @Test
  @StdIo
  @SuppressWarnings("JUnitMalformedDeclaration")
  void shouldRunCommandAndShowDocsWithInvalidConfig(StdOut out) throws Exception {
    var testSupport =
        new DropwizardTestSupport<>(
            ConfigurationSubstitutionBundleGenericConfigTest.TestApp.class,
            new ConfigurationSubstitutionBundleGenericConfigTest.TestConfiguration()
                .setForTestingCommandOnly(null),
            app -> new GenericLookupConfigCommand<>());
    try {
      testSupport.before();
      await()
          .untilAsserted(
              () ->
                  assertThat(String.join("\n", out.capturedLines()))
                      .contains("All supported environment variable keys:")
                      .contains("SERVER_GZIP_MINIMUMENTITYSIZE (DataSize)"));
    } finally {
      testSupport.after();
    }
  }
}
