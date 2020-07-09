package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dropwizard.Configuration;
import io.dropwizard.logging.AppenderFactory;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.logging.DefaultLoggingFactory;
import io.dropwizard.logging.json.AccessJsonLayoutBaseFactory;
import io.dropwizard.logging.json.EventJsonLayoutBaseFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sdase.commons.server.dropwizard.bundles.test.LoggingTestApp;
import org.sdase.commons.server.testing.EnvironmentRule;

public class DefaultLoggingConfigurationBundleWithJsonLoggingEnabledTest {

  public static final EnvironmentRule ENV =
      new EnvironmentRule().setEnv("ENABLE_JSON_LOGGING", "true");

  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          LoggingTestApp.class, resourceFilePath("without-appenders-key-config.yaml"));

  @ClassRule public static final RuleChain RULE = RuleChain.outerRule(ENV).around(DW);

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
    assertThat(consoleAppenderFactory.getLayout()).isInstanceOf(EventJsonLayoutBaseFactory.class);

    EventJsonLayoutBaseFactory layout =
        (EventJsonLayoutBaseFactory) consoleAppenderFactory.getLayout();

    assertThat(layout.isFlattenMdc()).isFalse();
  }

  @Test()
  public void shouldApplyRequestLogConsoleAppender() {
    LoggingTestApp app = DW.getApplication();

    DefaultServerFactory serverFactory =
        (DefaultServerFactory) app.getConfiguration().getServerFactory();
    LogbackAccessRequestLogFactory requestLogFactory =
        (LogbackAccessRequestLogFactory) serverFactory.getRequestLogFactory();
    ConsoleAppenderFactory consoleAppenderFactory =
        (ConsoleAppenderFactory) requestLogFactory.getAppenders().get(0);

    assertThat(consoleAppenderFactory.getLayout()).isInstanceOf(AccessJsonLayoutBaseFactory.class);
    AccessJsonLayoutBaseFactory accessJsonLayoutBaseFactory =
        (AccessJsonLayoutBaseFactory) consoleAppenderFactory.getLayout();

    assertThat(accessJsonLayoutBaseFactory.getRequestHeaders())
        .containsExactly("Trace-Token", "Consumer-Token");
    assertThat(accessJsonLayoutBaseFactory.getResponseHeaders()).containsExactly("Trace-Token");
  }
}
