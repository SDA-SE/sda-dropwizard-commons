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
