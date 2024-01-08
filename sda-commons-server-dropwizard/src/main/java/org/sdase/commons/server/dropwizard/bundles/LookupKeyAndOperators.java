package org.sdase.commons.server.dropwizard.bundles;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides the {@link #getKey() actual lookup key} and {@linkplain #getOperators() optional
 * operators} from a full lookup key string.
 *
 * @see #of(String)
 */
public class LookupKeyAndOperators {

  private final String key;
  private final List<String> operators;

  /**
   * Creates a new instance of {@link LookupKeyAndOperators}
   *
   * @param keyAndOperators a key with optional operators, e.g.
   *     <ul>
   *       <li>{@code FOO_KEY}
   *       <li>{@code FOO_KEY | toJsonString}
   *     </ul>
   *
   * @return a {@link LookupKeyAndOperators} instance with access to the {@link #getKey() actual
   *     lookup key} and {@linkplain #getOperators() optional operators}
   */
  public static LookupKeyAndOperators of(String keyAndOperators) {
    return new LookupKeyAndOperators(keyAndOperators);
  }

  /**
   * Extracts the actual lookup key of a key with operators.
   *
   * @param keyAndOperators The key with operators as defined in the String with substitutions.
   *     Examples:
   *     <ul>
   *       <li><code>foo: ${FOO_PROPERTY}</code> =&gt; keyAndOperators = {@code FOO_PROPERTY}
   *       <li><code>foo: ${FOO_PROPERTY:-default}</code> =&gt; keyAndOperators = {@code
   *           FOO_PROPERTY}
   *       <li><code>foo: ${FOO_PROPERTY | toJsonString:-default}</code> =&gt; keyAndOperators =
   *           {@code FOO_PROPERTY | toJsonString}
   *     </ul>
   *
   * @return The key that should be looked up in the environment. All examples for {@code
   *     keyAndOperators} above will return {@code FOO_PROPERTY}.
   */
  public static String lookupKey(String keyAndOperators) {
    return of(keyAndOperators).key;
  }

  /**
   * @see #of(String)
   * @param keyAndOperators The key with operators as defined in the String with substitutions.
   *     Examples:
   *     <ul>
   *       <li><code>foo: ${FOO_PROPERTY}</code> =&gt; keyAndOperators = {@code FOO_PROPERTY}
   *       <li><code>foo: ${FOO_PROPERTY:-default}</code> =&gt; keyAndOperators = {@code
   *           FOO_PROPERTY}
   *       <li><code>foo: ${FOO_PROPERTY | toJsonString:-default}</code> =&gt; keyAndOperators =
   *           {@code FOO_PROPERTY | toJsonString}
   *     </ul>
   */
  private LookupKeyAndOperators(String keyAndOperators) {
    String[] keyAndOperatorsArray = keyAndOperators.split("\\|");
    this.key = keyAndOperatorsArray[0].trim();
    this.operators =
        Arrays.asList(keyAndOperatorsArray).subList(1, keyAndOperatorsArray.length).stream()
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toList();
  }

  public String getKey() {
    return key;
  }

  public List<String> getOperators() {
    return operators;
  }
}
