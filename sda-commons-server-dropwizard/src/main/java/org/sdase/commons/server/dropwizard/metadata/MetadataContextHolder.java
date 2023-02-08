package org.sdase.commons.server.dropwizard.metadata;

class MetadataContextHolder {

  private static final ThreadLocal<MetadataContext> METADATA_CONTEXT = new ThreadLocal<>();

  static MetadataContext get() {
    return getInternal();
  }

  static void set(MetadataContext metadataContext) {
    METADATA_CONTEXT.set(metadataContext);
  }

  static void clear() {
    METADATA_CONTEXT.remove();
  }

  private static MetadataContext getInternal() {
    synchronized (Thread.currentThread()) {
      if (METADATA_CONTEXT.get() == null) {
        METADATA_CONTEXT.set(new UnmodifiableMetadataContext());
      }
    }
    return METADATA_CONTEXT.get();
  }
}
