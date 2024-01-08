package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.core.Configuration;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.dropwizard.bundles.test.LoggingTestApp;

class DefaultLoggingConfigurationBundleWithoutAppendersKeyTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          LoggingTestApp.class, resourceFilePath("without-appenders-key-config.yaml"));

  @Test
  void shouldApplyConsoleAppender() {
    LoggingTestApp app = DW.getApplication();
    DefaultLoggingFactory defaultLoggingFactory =
        (DefaultLoggingFactory) app.getConfiguration().getLoggingFactory();

    assertThat(defaultLoggingFactory.getLevel()).isEqualTo("WARN");

    List<AppenderFactory<ILoggingEvent>> consoleAppenderFactories =
        defaultLoggingFactory.getAppenders().stream()
            .filter(a -> a instanceof ConsoleAppenderFactory)
            .toList();

    assertThat(consoleAppenderFactories.size()).isEqualTo(1);

    ConsoleAppenderFactory<ILoggingEvent> consoleAppenderFactory =
        (ConsoleAppenderFactory<ILoggingEvent>) consoleAppenderFactories.get(0);

    assertThat(consoleAppenderFactory.getLogFormat())
        .isEqualTo("[%d] [%-5level] [%X{Trace-Token}] %logger{36} - %msg%n");
    assertThat(consoleAppenderFactory.getThreshold()).isEqualTo("TRACE");
    assertThat(consoleAppenderFactory.getLayout()).isNull();
  }
}
