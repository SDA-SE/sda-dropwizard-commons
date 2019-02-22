package org.sdase.commons.server.dropwizard.logging;

import static java.util.Objects.requireNonNull;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import io.dropwizard.configuration.ConfigurationSourceProvider;

public class ConsoleAppenderInjectorSourceProvider implements ConfigurationSourceProvider {

   private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
   private static final String DEFAULT_LOG_FORMAT = "[%d] [%-5level] [%X{Trace-Token}] %logger{36} - %msg%n";
   private static final String DEFAULT_THRESHOLD = "INFO";

   private final ConfigurationSourceProvider delegate;

   /**
    * Create a new instance.
    *
    * @param delegate
    *           The underlying {@link ConfigurationSourceProvider}.
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
         Map<String, Object> root = (Map<String, Object>) yaml.load(in);

         try {
            applyLogging(root);
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
      applyAppenders(logging);
   }

   private void applyAppenders(Map<String, Object> logging) {
      logging.putIfAbsent("appenders", new ArrayList<>());
      @SuppressWarnings("unchecked")
      List<Object> appenders = (List<Object>) logging.get("appenders");
      applyConsoleAppender(appenders);
   }

   private void applyConsoleAppender(List<Object> appenders) {
      AtomicBoolean foundConsoleAppender = new AtomicBoolean();
      appenders.forEach(a -> {
         if (a instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> appender = (Map<String, Object>) a;

            if ("console".equals((appender).get("type"))) {
               applyConsoleAppenderDefaults(appender);
               foundConsoleAppender.set(true);
            }
         }
      });

      if (!foundConsoleAppender.get()) {
         Map<String, Object> consoleAppender = createConsoleAppender();
         applyConsoleAppenderDefaults(consoleAppender);
         appenders.add(consoleAppender);
      }
   }

   private Map<String, Object> createConsoleAppender() {
      Map<String, Object> consoleAppender = new HashMap<>();
      consoleAppender.put("type", "console");
      return consoleAppender;
   }

   private void applyConsoleAppenderDefaults(Map<String, Object> consoleAppender) {
      consoleAppender.putIfAbsent("threshold", DEFAULT_THRESHOLD);
      consoleAppender.putIfAbsent("logFormat", DEFAULT_LOG_FORMAT);

      if (!DEFAULT_LOG_FORMAT.equals(consoleAppender.get("logFormat"))) {
         LOG
               .warn("The current console appender log format does not comply to the format expected "
                     + "by our logging pipeline. This might cause unexpected behavior.");
      }
   }
}
