package org.sdase.commons.server.dropwizard.metadata;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * The {@code MetadataContext} stores information about a long-running business processes that is
 * independent of specific APIs or business rules of a single service. A {@code MetaContext} is
 * built from the metadata fields of input APIs like HTTP or Kafka message headers. It is added
 * automatically to output APIs when using platform clients from the {@code JerseyClientBundle} or
 * the {@code KafkaMessageProducer}.
 *
 * <p>Services that claim to support {@code MetadataContext} must take care that it is also kept and
 * reloaded when the business process is interrupted and proceeded later. An interrupted business
 * process in these terms is for example asynchronous processing in a new {@link Thread} or
 * finishing the current step of the process by saving the current state to a database.
 *
 * <p>The metadata fields (e.g. header names) that are put into the {@code MetadataContext} must be
 * configured by {@linkplain System#getenv(String) environment variable} or {@linkplain
 * System#getProperty(String) system property} {@value METADATA_FIELDS_ENVIRONMENT_VARIABLE} as
 * comma separated list of header names, e.g. {@code business-process-id,tenant-id}. Header names
 * are treated case-insensitive.
 *
 * <p>Metadata supports to have multiple values per key. Commas (@code ,) are not allowed in values
 * to support composition as described in <a
 * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-field-lines-and-combined-fi">RFC 9110
 * 5.2</a>.
 *
 * <p>Technically, the {@code MetadataContext} is implemented as static {@link ThreadLocal}. When
 * code is executed in a new {@link Thread}, the context must be transferred. Helper methods are
 * available for {@linkplain #transferMetadataContext(Runnable) <code>Runnable</code>} and
 * {@linkplain #transferMetadataContext(Callable) <code>Callable</code>}
 */
// TODO not picked up from Kafka consumers as documented yet.
// TODO Not handled in platform client and KafkaMessageProducer as documented yet.
public interface MetadataContext {

  /**
   * The name of the property or environment variable that defines which fields (e.g. of HTTP
   * request headers or Kafka message headers are) included in the {@link MetadataContext}. The
   * value must be a comma separated list of field names.
   *
   * <p>Example: {@code METADATA_FIELDS=tenant-id,processes}
   */
  String METADATA_FIELDS_ENVIRONMENT_VARIABLE = "METADATA_FIELDS";

  /**
   * @return The fields that should be included in the {@link MetadataContext}, derived from the
   *     property or environment variable {@value #METADATA_FIELDS_ENVIRONMENT_VARIABLE}.
   */
  static Set<String> metadataFields() {
    return MetadataContextUtil.metadataFields();
  }

  /**
   * @return the immutable metadata context of the current {@link Thread}, never {@code null}.
   */
  static MetadataContext current() {
    return MetadataContextUtil.current();
  }

  /**
   * @return the current metadata context, never {@code null}. Changes in the returned instance will
   *     not affect the {@link MetadataContext} of the current {@link Thread}.
   */
  static DetachedMetadataContext detachedCurrent() {
    return DetachedMetadataContext.of(MetadataContextUtil.current());
  }

  /**
   * Creates a new {@link MetadataContext} for the current {@link Thread}. Any existing {@link
   * MetadataContext} in the current {@link Thread} will be replaced.
   *
   * @param metadataContext the new {@link MetadataContext}, e.g. {@linkplain
   *     DetachedMetadataContext#toMetadataContext() derived} from a {@link
   *     DetachedMetadataContext}.
   */
  static void createContext(DetachedMetadataContext metadataContext) {
    MetadataContextHolder.set(metadataContext.toMetadataContext());
  }

  /**
   * {@linkplain #createContext(DetachedMetadataContext) Creates a new current context} that is
   * alive until the returned {@link Closeable} is {@linkplain Closeable#close() closed}. On
   * closing, the previous context will be restored.
   *
   * <p>Example usage:
   *
   * <pre>
   *   <code>
   *     var myContext = // create a context from something
   *     try (var ignored = MetadataContext.createCloseableContext(myContext)) {
   *       // do something
   *     }
   *   </code>
   * </pre>
   *
   * @param metadataContext the context to be set until the returned closable is {@linkplain
   *     Closeable#close() closed}
   * @return a closeable that will restore the previous context when {@linkplain Closeable#close()
   *     closed}
   */
  static MetadataContextCloseable createCloseableContext(DetachedMetadataContext metadataContext) {
    var previous = detachedCurrent();
    createContext(metadataContext);
    return new MetadataContextCloseable(previous);
  }

  /**
   * Merges the given new metadata context data into the {@link #current()} metadata context.
   *
   * @param newContextData the context data that should be merged into the {@linkplain #current()
   *     current context}
   * @param mergeStrategy defines the preference when metadata for a specific key exists in both
   *     contexts
   * @throws NullPointerException if {@code mergeStrategy} is {@code null}
   */
  static void mergeContext(
      DetachedMetadataContext newContextData, MetadataContextMergeStrategy mergeStrategy) {
    var current = detachedCurrent();
    var newNonNull = Optional.ofNullable(newContextData).orElse(new DetachedMetadataContext());
    switch (mergeStrategy) {
      case EXTEND:
        createContext(MetadataContextUtil.merge(current, newNonNull));
        break;
      case REPLACE:
        createContext(MetadataContextUtil.mergeWithPreference(newNonNull, current));
        break;
      case KEEP:
        createContext(MetadataContextUtil.mergeWithPreference(current, newNonNull));
        break;
    }
  }

  /**
   * Transfers the current metadata context to the runnable when executed in a new thread.
   *
   * @param runnable The runnable to wrap with the current metadata context.
   * @return The original runnable wrapped with code to transfer the metadata context when executed
   *     in a new thread.
   */
  static Runnable transferMetadataContext(Runnable runnable) {
    return MetadataContextUtil.transferMetadataContext(runnable);
  }

  /**
   * Transfers the current metadata context to the callable when executed in a new thread.
   *
   * @param callable The runnable to wrap with the current metadata context.
   * @return The original callable wrapped with code to transfer the metadata context when executed
   *     in a new thread.
   */
  static <V> Callable<V> transferMetadataContext(Callable<V> callable) {
    return MetadataContextUtil.transferMetadataContext(callable);
  }

  /**
   * Derives the metadata key from the service specific configured environment.
   *
   * <p>This is the preferred approach to determine the key when adding information to the metadata
   * context.
   *
   * @param environmentOrPropertyName environmentOrPropertyName the name of a {@linkplain
   *     System#getProperty(String) system property} or {@linkplain System#getenv(String)
   *     environment variable} that defines the actual key for a specific need of the service.
   * @return the metadata key that is used for the metadata context
   * @throws KeyConfigurationMissingException if the given {@code environmentOrPropertyName} does *
   *     not resolve to a metadata context key
   */
  static String keyFromConfiguration(String environmentOrPropertyName)
      throws KeyConfigurationMissingException {
    return MetadataContextUtil.keyFromConfiguration(environmentOrPropertyName);
  }

  /**
   * @return all available keys in the metadata context.
   */
  Set<String> keys();

  /**
   * {@link #valuesByKeyFromEnvironment(String)} should be preferred to be independent of different
   * environments.
   *
   * @param key a key in the metadata context
   * @return the values stored in the metadata context by this key
   */
  List<String> valuesByKey(String key);

  /**
   * {@linkplain #valuesByKey(String) reads} the values of the metadata context from a key that is
   * configurable by {@linkplain System#getProperty(String) system properties} or {@linkplain
   * System#getenv(String) environment variables}. This is the preferred approach to get information
   * from the metadata context for a specific need.
   *
   * <p>The {@code environmentOrPropertyName} should refer to the business use of the context values
   * in a specific service. The actual key is dependent on the environment where the service is
   * used. Other services may need the same key in a different context and operators may have their
   * own assumptions about the naming.
   *
   * @param environmentOrPropertyName the name of a {@linkplain System#getProperty(String) system
   *     property} or {@linkplain System#getenv(String) environment variable} that defines the
   *     actual key for a specific need of the service.
   * @return the values stored in the metadata context by this key
   * @throws KeyConfigurationMissingException if the given {@code environmentOrPropertyName} does
   *     not resolve to a metadata context key
   */
  default List<String> valuesByKeyFromEnvironment(String environmentOrPropertyName)
      throws KeyConfigurationMissingException {
    return valuesByKey(keyFromConfiguration(environmentOrPropertyName));
  }

  /**
   * @return {@code true}, if no metadata {@link #keys()} are defined or no {@link
   *     #valuesByKey(String)} contain data, {@code false} if any {@link #valuesByKey(String)} is
   *     not {@linkplain List#isEmpty() empty}
   */
  default boolean isEffectivelyEmpty() {
    return keys().isEmpty()
        || keys().stream().map(this::valuesByKey).filter(Objects::nonNull).allMatch(List::isEmpty);
  }
}
