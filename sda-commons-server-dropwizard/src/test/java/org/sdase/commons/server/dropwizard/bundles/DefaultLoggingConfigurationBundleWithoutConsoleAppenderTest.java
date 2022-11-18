package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.Configuration;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.dropwizard.bundles.test.LoggingTestApp;

class DefaultLoggingConfigurationBundleWithoutConsoleAppenderTest {

  @RegisterExtension
  private static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          LoggingTestApp.class, resourceFilePath("without-console-appender-config.yaml"));

  @Test
  void shouldApplyConsoleAppender() {
    LoggingTestApp app = DW.getApplication();
    DefaultLoggingFactory defaultLoggingFactory =
        (DefaultLoggingFactory) app.getConfiguration().getLoggingFactory();

    List<AppenderFactory<ILoggingEvent>> consoleAppenderFactories =
        defaultLoggingFactory.getAppenders().stream()
            .filter(a -> a instanceof ConsoleAppenderFactory)
            .collect(Collectors.toList());

    assertThat(consoleAppenderFactories.size()).isEqualTo(1);

    ConsoleAppenderFactory<ILoggingEvent> consoleAppenderFactory =
        (ConsoleAppenderFactory<ILoggingEvent>) consoleAppenderFactories.get(0);

    assertThat(consoleAppenderFactory.getLogFormat())
        .isEqualTo("[%d] [%-5level] [%X{Trace-Token}] %logger{36} - %msg%n");
    assertThat(consoleAppenderFactory.getThreshold()).isEqualTo("INFO");
    assertThat(consoleAppenderFactory.getLayout()).isNull();
  }
}
