package org.sdase.commons.server.dropwizard.logging;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import io.dropwizard.configuration.ConfigurationSourceProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class ConsoleAppenderInjectorSourceProvider implements ConfigurationSourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String DEFAULT_LOG_FORMAT =
      "[%d] [%-5level] [%X{Trace-Token}] %logger{36} - %msg%n";
  private static final String DEFAULT_THRESHOLD = "INFO";

  private final ConfigurationSourceProvider delegate;

  /**
   * Create a new instance.
   *
   * @param delegate The underlying {@link ConfigurationSourceProvider}.
   */
  public ConsoleAppenderInjectorSourceProvider(ConfigurationSourceProvider delegate) {
    this.delegate = requireNonNull(delegate);
  }

  /** {@inheritDoc} */
  @Override
  public InputStream open(String path) throws IOException {
    try (final InputStream in = delegate.open(path)) {
      Yaml yaml = new Yaml();
      @SuppressWarnings("unchecked")
      Map<String, Object> root = yaml.load(in);

      try {
        if (root == null) {
          root = new HashMap<>();
        }

        applyLogging(root);
        applyServer(root);
      } catch (ClassCastException ex) {
        LOG.error("Unable to inject console appender config, invalid config format");
      }

      final String substituted = yaml.dump(root);
      return new ByteArrayInputStream(substituted.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void applyLogging(Map<String, Object> root) {
    root.putIfAbsent("logging", new HashMap<String, Object>());
    @SuppressWarnings("unchecked")
    Map<String, Object> logging = (Map<String, Object>) root.get("logging");
    applyLoggingAppenders(logging);
  }

  private void applyLoggingAppenders(Map<String, Object> logging) {
    logging.putIfAbsent("appenders", new ArrayList<>());
    @SuppressWarnings("unchecked")
    List<Object> appenders = (List<Object>) logging.get("appenders");
    applyLoggingConsoleAppender(appenders);
  }

  private void applyLoggingConsoleAppender(List<Object> appenders) {
    AtomicBoolean foundConsoleAppender = new AtomicBoolean();
    appenders.forEach(
        a -> {
          if (a instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> appender = (Map<String, Object>) a;

            if ("console".equals((appender).get("type"))) {
              applyLoggingConsoleAppenderDefaults(appender);
              foundConsoleAppender.set(true);
            }
          }
        });

    if (!foundConsoleAppender.get()) {
      Map<String, Object> consoleAppender = createLoggingConsoleAppender();
      applyLoggingConsoleAppenderDefaults(consoleAppender);
      appenders.add(consoleAppender);
    }
  }

  private Map<String, Object> createLoggingConsoleAppender() {
    Map<String, Object> consoleAppender = new HashMap<>();
    consoleAppender.put("type", "console");
    return consoleAppender;
  }

  private void applyLoggingConsoleAppenderDefaults(Map<String, Object> consoleAppender) {
    consoleAppender.putIfAbsent("threshold", DEFAULT_THRESHOLD);
    consoleAppender.putIfAbsent("logFormat", DEFAULT_LOG_FORMAT);

    if ("true".equals(new SystemPropertyAndEnvironmentLookup().lookup("ENABLE_JSON_LOGGING"))) {
      consoleAppender.putIfAbsent("layout", createLoggingConsoleAppenderLayout());
    }

    if (!DEFAULT_LOG_FORMAT.equals(consoleAppender.get("logFormat"))) {
      LOG.warn(
          "The current console appender log format does not comply to the format expected "
              + "by our logging pipeline. This might cause unexpected behavior.");
    }
  }

  private Map<String, Object> createLoggingConsoleAppenderLayout() {
    Map<String, Object> consoleAppenderLayout = new HashMap<>();
    consoleAppenderLayout.put("type", "json");
    return consoleAppenderLayout;
  }

  private void applyServer(Map<String, Object> root) {
    root.putIfAbsent("server", new HashMap<String, Object>());
    @SuppressWarnings("unchecked")
    Map<String, Object> server = (Map<String, Object>) root.get("server");
    applyRequestLog(server);
  }

  private void applyRequestLog(Map<String, Object> server) {
    server.putIfAbsent("requestLog", new HashMap<String, Object>());
    @SuppressWarnings("unchecked")
    Map<String, Object> requestLog = (Map<String, Object>) server.get("requestLog");
    applyRequestLogAppenders(requestLog);
  }

  private void applyRequestLogAppenders(Map<String, Object> requestLog) {
    requestLog.putIfAbsent("appenders", new ArrayList<>());
    @SuppressWarnings("unchecked")
    List<Object> appenders = (List<Object>) requestLog.get("appenders");
    applyRequestLogConsoleAppender(appenders);
  }

  private void applyRequestLogConsoleAppender(List<Object> appenders) {
    AtomicBoolean foundConsoleAppender = new AtomicBoolean();
    appenders.forEach(
        a -> {
          if (a instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> appender = (Map<String, Object>) a;

            if ("console".equals((appender).get("type"))) {
              applyRequestLogConsoleAppenderDefaults(appender);
              foundConsoleAppender.set(true);
            }
          }
        });

    if (!foundConsoleAppender.get()) {
      Map<String, Object> consoleAppender = createRequestLogConsoleAppender();
      applyRequestLogConsoleAppenderDefaults(consoleAppender);
      appenders.add(consoleAppender);
    }
  }

  private Map<String, Object> createRequestLogConsoleAppender() {
    Map<String, Object> consoleAppender = new HashMap<>();
    consoleAppender.put("type", "console");
    return consoleAppender;
  }

  private void applyRequestLogConsoleAppenderDefaults(Map<String, Object> consoleAppender) {
    if ("true".equals(new SystemPropertyAndEnvironmentLookup().lookup("ENABLE_JSON_LOGGING"))) {
      consoleAppender.putIfAbsent("layout", createRequestLogConsoleAppenderLayout());
    }

    if ("true"
        .equals(new SystemPropertyAndEnvironmentLookup().lookup("DISABLE_HEALTHCHECK_LOGS"))) {
      consoleAppender.putIfAbsent(
          "filterFactories", createRequestLogConsoleAppenderFilterFactories());
    }
  }

  private Map<String, Object> createRequestLogConsoleAppenderLayout() {
    Map<String, Object> consoleAppenderLayout = new HashMap<>();
    consoleAppenderLayout.put("type", "access-json");
    consoleAppenderLayout.put("requestHeaders", asList("Trace-Token", "Consumer-Token"));
    consoleAppenderLayout.put("responseHeaders", singletonList("Trace-Token"));
    return consoleAppenderLayout;
  }

  private List<Map<String, Object>> createRequestLogConsoleAppenderFilterFactories() {
    List<Map<String, Object>> filterFactories = new ArrayList<>();
    Map<String, Object> uriFilter = new HashMap<>();
    uriFilter.put("type", "uri");
    uriFilter.put("uris", asList("/ping", "/healthcheck/internal"));
    filterFactories.add(uriFilter);

    return filterFactories;
  }
}
