package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.Configuration;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.dropwizard.bundles.test.LoggingTestApp;

public class DefaultLoggingConfigurationBundleWithUnconfiguredConsoleAppenderTest {

  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          LoggingTestApp.class, resourceFilePath("with-unconfigured-console-appender-config.yaml"));

  @Test()
  public void shouldApplyConsoleAppender() {
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
  }
}
