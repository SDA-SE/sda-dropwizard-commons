package org.sdase.commons.server.dropwizard.bundles;

import com.fasterxml.jackson.core.io.CharTypes;
import java.util.Optional;
import org.apache.commons.text.lookup.StringLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link StringLookup} for Java's system properties and environment variables. System properties
 * have higher priority. While the use of environment variables is the typical approach to configure
 * services in a container, system properties are used in tests because it is not possible to modify
 * environment variables in recent Java versions.
 *
 * <p>Looked up values can be modified by operators. The actual key identified by the {@link
 * org.apache.commons.text.StringSubstitutor} may use a piped syntax to apply operators to the
 * looked up value. This {@link StringLookup} will extract the key and the operators and applies
 * them.
 *
 * <p>Currently supported operators:
 *
 * <ul>
 *   <li>{@code toJsonString}: Builds a Json String that exactly represents the looked up value.
 *       Example:
 *       <pre>
 *       <code>property: ${PROPERTY | toJsonString:-null}</code>
 *     </pre>
 *       The example will look up {@code PROPERTY}. Assuming it resolves to {@code some"string}, the
 *       result will be:
 *       <pre>
 *       <code>property: "some\"string"</code>
 *     </pre>
 * </ul>
 */
public class SystemPropertyAndEnvironmentLookup implements StringLookup {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SystemPropertyAndEnvironmentLookup.class);

  private static final String TO_JSON_STRING_OPERATOR_NORMALIZED = "toJsonString".toLowerCase();

  @Override
  public String lookup(String key) {
    LookupKeyAndOperators keyAndOperators = LookupKeyAndOperators.of(key);
    String result = lookupValue(keyAndOperators);
    if (result == null) {
      return null;
    }
    return modifyValue(keyAndOperators, result);
  }

  private String modifyValue(LookupKeyAndOperators lookupKeyAndOperators, String originalValue) {
    for (String operator : lookupKeyAndOperators.getOperators()) {
      String normalizedOperator = operator.toLowerCase();
      // refactor to switch-case and separate operator classes when more operators are added
      if (TO_JSON_STRING_OPERATOR_NORMALIZED.equals(normalizedOperator)) {
        originalValue = toJsonString(originalValue);
      } else {
        LOGGER.error(
            "Ignoring unknown substitution operator '{}' when replacing {}",
            operator,
            lookupKeyAndOperators.getKey());
      }
    }
    return originalValue;
  }

  private String lookupValue(LookupKeyAndOperators lookupKeyAndOperators) {
    String keyToLookup = lookupKeyAndOperators.getKey();
    return Optional.ofNullable(System.getProperty(keyToLookup)).orElse(System.getenv(keyToLookup));
  }

  private String toJsonString(String source) {
    // inspired by com.fasterxml.jackson.databind.node.TextNode#appendQuoted(StringBuilder, String)
    StringBuilder quoted = new StringBuilder();
    CharTypes.appendQuoted(quoted, source);
    return "\"" + quoted + "\"";
  }
}
