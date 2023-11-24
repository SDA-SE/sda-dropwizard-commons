package org.sdase.commons.server.dropwizard.bundles.configuration.generic;

import static org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner.DROPWIZARD_PLAIN_TYPES;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import java.util.stream.Collectors;
import net.sourceforge.argparse4j.inf.Namespace;
import org.sdase.commons.server.dropwizard.bundles.scanner.JacksonTypeScanner;

/**
 * A {@link io.dropwizard.cli.Command} that documents the configuration keys as {@linkplain
 * System#out standard output} that are discovered dynamically by the {@link JacksonTypeScanner}.
 *
 * @param <T> the type of the configuration class of this service
 */
public class GenericLookupConfigCommand<T extends Configuration> extends ConfiguredCommand<T> {

  private static final String HELP_TEMPLATE =
      // next major: make it a multiline String when Java 11 is not supported anymore
      String.join(
          "%n",
          "Configuration can be manipulated dynamically from environment variables.",
          "Some types use a special syntax for the value:",
          "",
          "- Duration: `<int><unit>` with unit as:",
          "  - `ns`: nanoseconds",
          "  - `us`: microseconds",
          "  - `ms`: milliseconds",
          "  - `s`: seconds",
          "  - `m`: minutes",
          "  - `h`: hours",
          "  - `d`: days",
          "- DataSize: `<int><unit>`, with unit as",
          "  - `B`: bytes",
          "  - `KB`: kilobytes",
          "  - `KiB`: kibibytes",
          "  - `MB`: megabytes",
          "  - `MiB`: mebibytes",
          "  - `GB`: gigabytes",
          "  - `GiB`: gibibytes",
          "  - `T`: terabytes",
          "  - `TiB`: tebibytes",
          "  - `PB`: petabytes",
          "  - `PiB`: pebibytes",
          "",
          "The type Map supports String values and uses `<KEY>` in the environment variable.",
          "`<KEY>` must be changed to the desired key of the Map using the character case as needed.",
          "`<KEY>` may occur in other types to fill a Map of well defined objects.",
          "",
          "The type Array supports String values and uses `<INDEX>` in the environment variable.",
          "`<INDEX>` must be changed to the desired zero-based index of the array or collection.",
          "`<INDEX>` may occur in other types to fill an array of well defined objects.",
          "",
          "The type JsonNode supports nested Structures of Maps, Arrays and String values.",
          "The documented path contains `<ANY>` which can be the key of a Map, an index of an Array",
          "or omitted to set a String.",
          "If omitted, the preceding underscore (`_`) must be omitted as well.",
          "`<ANY>` is also placeholder to build a hierarchy with keys or indexes separated by",
          "underscores.",
          "Therefore, keys of such Maps can't contain underscores.",
          "",
          "The following dynamically discovered environment variables may interfere with pre-defined",
          "variables in the configuration yaml file:",
          "",
          "%s",
          "");

  /**
   * Discovers all dynamic environment properties and creates a human-readable multiline
   * documentation text with information about types, formats and conventions used in the current
   * implementation followed by a list. The content renders as markdown.
   *
   * @param objectMapper the {@link ObjectMapper} that is configured similar to the one used on
   *     startup for parsing the configuration.
   * @param configurationClass the class to configure the service
   * @return human-readable multiline documentation text that is compatible with markdown
   * @param <C> the class to configure the service
   */
  public static <C extends Configuration> String createDynamicPropertyDocumentation(
      ObjectMapper objectMapper, Class<C> configurationClass) {
    String configurationHints =
        new JacksonTypeScanner(objectMapper, DROPWIZARD_PLAIN_TYPES)
            .createConfigurationHints(configurationClass);
    String propertiesAsList =
        configurationHints
            .lines()
            .map(l -> String.format("- `%s`", l))
            .collect(Collectors.joining("\n"));
    return String.format(HELP_TEMPLATE, propertiesAsList);
  }

  public GenericLookupConfigCommand() {
    super("config", "Shows available generic environment variables for configuration.");
  }

  @Override
  @SuppressWarnings("java:S106") // using System.out to avoid clutter in output
  protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration)
      throws Exception {
    // Use generic type parameter from reflection.
    // configuration.getClass() does not work in Docker (for unknown reason)
    System.out.println(
        createDynamicPropertyDocumentation(
            bootstrap.getObjectMapper(), bootstrap.getApplication().getConfigurationClass()));
  }
}
