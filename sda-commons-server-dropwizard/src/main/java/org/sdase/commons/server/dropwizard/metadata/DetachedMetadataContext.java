package org.sdase.commons.server.dropwizard.metadata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * A {@link MetadataContext} representation which is detached from the {@linkplain
 * MetadataContext#current() current} {@link MetadataContext}. Changes in this class do not
 * propagate to the {@linkplain MetadataContext#current() current} {@link MetadataContext} and vice
 * versa.
 *
 * <p>This variant of a {@link MetadataContext} can be added to a business entity to persist the
 * context related to that entity in MongoDB with {@code sda-commons-server-spring-data-mongo}. When
 * a process continues and the entity is loaded from MongoDB, the {@link MetadataContext} can be
 * restored like this:
 *
 * <pre>{@code MetadataContext.createContext(detachedMetadataContextFromEntity)}</pre>
 */
public class DetachedMetadataContext extends LinkedHashMap<String, List<String>> {

  /**
   * @param metadataContext the source metadata
   * @return a {@link DetachedMetadataContext} that provides the information of the given {@code
   *     metadataContext} without affecting it on changes
   */
  public static DetachedMetadataContext of(MetadataContext metadataContext) {
    var target = new DetachedMetadataContext();
    for (String metadataKey : metadataContext.keys()) {
      target.put(metadataKey, new ArrayList<>(metadataContext.valuesByKey(metadataKey)));
    }
    return target;
  }

  /**
   * @return a {@link MetadataContext} to {@linkplain
   *     MetadataContext#createContext(DetachedMetadataContext) create a new context for the current
   *     thread}
   */
  public MetadataContext toMetadataContext() {
    return UnmodifiableMetadataContext.of(this);
  }

  /**
   * @return {@code true}, if no metadata {@linkplain #keySet() keys} are defined or no {@linkplain
   *     #get(Object) values} contain data, {@code false} if any {@linkplain #get(Object) value} is
   *     not {@linkplain List#isEmpty() empty}
   */
  public boolean isEffectivelyEmpty() {
    return keySet().isEmpty()
        || keySet().stream().map(this::get).filter(Objects::nonNull).allMatch(List::isEmpty);
  }
}
