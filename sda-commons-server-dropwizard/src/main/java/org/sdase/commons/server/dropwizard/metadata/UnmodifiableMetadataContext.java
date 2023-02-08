package org.sdase.commons.server.dropwizard.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class UnmodifiableMetadataContext implements MetadataContext {

  private final DetachedMetadataContext detachedMetadataContext = new DetachedMetadataContext();

  static UnmodifiableMetadataContext of(DetachedMetadataContext source) {
    var target = new UnmodifiableMetadataContext();
    for (var e : source.entrySet()) {
      target.detachedMetadataContext.put(e.getKey(), new ArrayList<>(e.getValue()));
    }
    return target;
  }

  @Override
  public Set<String> keys() {
    return detachedMetadataContext.keySet();
  }

  @Override
  public List<String> valuesByKey(String key) {
    return detachedMetadataContext.get(key);
  }
}
