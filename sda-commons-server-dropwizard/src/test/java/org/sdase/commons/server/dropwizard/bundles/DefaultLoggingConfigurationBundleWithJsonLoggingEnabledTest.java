package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.ConsoleAppenderFactory;
import io.dropwizard.logging.common.DefaultLoggingFactory;
import io.dropwizard.logging.json.AccessJsonLayoutBaseFactory;
import io.dropwizard.logging.json.EventJsonLayoutBaseFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.dropwizard.bundles.test.LoggingTestApp;

@SetSystemProperty(key = "ENABLE_JSON_LOGGING", value = "true")
class DefaultLoggingConfigurationBundleWithJsonLoggingEnabledTest {

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
            .collect(Collectors.toList());

    assertThat(consoleAppenderFactories.size()).isEqualTo(1);

    ConsoleAppenderFactory<ILoggingEvent> consoleAppenderFactory =
        (ConsoleAppenderFactory<ILoggingEvent>) consoleAppenderFactories.get(0);

    assertThat(consoleAppenderFactory.getLogFormat())
        .isEqualTo("[%d] [%-5level] [%X{Trace-Token}] %logger{36} - %msg%n");
    assertThat(consoleAppenderFactory.getThreshold()).isEqualTo("TRACE");
    assertThat(consoleAppenderFactory.getLayout()).isInstanceOf(EventJsonLayoutBaseFactory.class);

    EventJsonLayoutBaseFactory layout =
        (EventJsonLayoutBaseFactory) consoleAppenderFactory.getLayout();

    assertThat(layout.isFlattenMdc()).isFalse();
  }

  @Test
  void shouldApplyRequestLogConsoleAppender() {
    LoggingTestApp app = DW.getApplication();

    DefaultServerFactory serverFactory =
        (DefaultServerFactory) app.getConfiguration().getServerFactory();
    LogbackAccessRequestLogFactory requestLogFactory =
        (LogbackAccessRequestLogFactory) serverFactory.getRequestLogFactory();
    var consoleAppenderFactory =
        (ConsoleAppenderFactory<IAccessEvent>) requestLogFactory.getAppenders().get(0);

    assertThat(consoleAppenderFactory.getLayout()).isInstanceOf(AccessJsonLayoutBaseFactory.class);
    AccessJsonLayoutBaseFactory accessJsonLayoutBaseFactory =
        (AccessJsonLayoutBaseFactory) consoleAppenderFactory.getLayout();

    assertThat(accessJsonLayoutBaseFactory.getRequestHeaders())
        .containsExactly("Trace-Token", "Consumer-Token");
    assertThat(accessJsonLayoutBaseFactory.getResponseHeaders()).containsExactly("Trace-Token");
  }
}
