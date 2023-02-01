package org.sdase.commons.server.dropwizard.bundles;

import com.fasterxml.jackson.core.io.CharTypes;
import java.util.Optional;
import org.apache.commons.text.lookup.StringLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lookup for Java's system properties and environment variables. System properties have higher
 * priority.
 */
public class SystemPropertyAndEnvironmentLookup implements StringLookup {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SystemPropertyAndEnvironmentLookup.class);

  private static final String TO_JSON_STRING_OPERATOR_NORMALIZED = "toJsonString".toLowerCase();

  /**
   * Provides the actual key to lookup without any operators.
   *
   * @param definedKey The key with operators as defined in the String with substitutions. Examples:
   *     <ul>
   *       <li><code>foo: ${FOO_PROPERTY}</code> =&gt; definedKey = {@code FOO_PROPERTY}
   *       <li><code>foo: ${FOO_PROPERTY:-default}</code> =&gt; definedKey = {@code FOO_PROPERTY}
   *       <li><code>foo: ${FOO_PROPERTY | toJsonString:-default}</code> =&gt; definedKey = {@code
   *           FOO_PROPERTY | toJsonString}
   *     </ul>
   *
   * @return The key that should be looked up in the environment. All examples for {@code
   *     definedKey} above will return {@code FOO_PROPERTY}.
   */
  public static String extractKeyToLookup(String definedKey) {
    return splitKeyInKeyAndOperators(definedKey)[0].trim();
  }

  private static String[] splitKeyInKeyAndOperators(String definedKey) {
    return definedKey.split("\\|");
  }

  @Override
  public String lookup(String key) {
    String[] keyAndOperators = splitKeyInKeyAndOperators(key);
    String result = lookupValue(keyAndOperators);
    if (result == null) {
      return null;
    }
    return modifyValue(keyAndOperators, result);
  }

  private String modifyValue(String[] keyAndOperators, String originalValue) {
    for (int i = 1; i < keyAndOperators.length; i++) {
      String operator = keyAndOperators[i].trim();
      String normalizedOperator = operator.toLowerCase();
      // refactor to switch-case and separate operator classes when more operators are added
      if (TO_JSON_STRING_OPERATOR_NORMALIZED.equals(normalizedOperator)) {
        originalValue = toJsonString(originalValue);
      } else {
        LOGGER.error(
            "Ignoring unknown substitution operator '{}' when replacing {}",
            operator,
            keyAndOperators[0]);
      }
    }
    return originalValue;
  }

  private String lookupValue(String[] keyAndOperator) {
    String keyToLookup = keyAndOperator[0].trim();
    return Optional.ofNullable(System.getProperty(keyToLookup)).orElse(System.getenv(keyToLookup));
  }

  private String toJsonString(String source) {
    StringBuilder quoted = new StringBuilder();
    CharTypes.appendQuoted(quoted, source);
    return "\"" + quoted + "\"";
  }
}
