package org.sdase.commons.server.dropwizard.metadata;

import java.io.Closeable;

/**
 * A {@link Closeable} that restores a {@link MetadataContext} without throwing an {@link Exception}
 * when it {@linkplain #close() closes}.
 */
public class MetadataContextCloseable implements Closeable {

  private final DetachedMetadataContext detachedMetadataContext;

  MetadataContextCloseable(DetachedMetadataContext detachedMetadataContextToRestore) {
    this.detachedMetadataContext = detachedMetadataContextToRestore;
  }

  @Override
  public void close() {
    MetadataContext.createContext(detachedMetadataContext);
  }
}
