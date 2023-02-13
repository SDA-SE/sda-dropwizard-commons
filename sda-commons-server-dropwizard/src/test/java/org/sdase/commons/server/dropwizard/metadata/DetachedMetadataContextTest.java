package org.sdase.commons.server.dropwizard.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class DetachedMetadataContextTest {

  DetachedMetadataContext detachedMetadataContext = new DetachedMetadataContext();

  @Test
  void shouldIdentifyEmptyMetadata() {
    assertThat(detachedMetadataContext.isEffectivelyEmpty()).isTrue();
  }

  @Test
  void shouldIdentifyEmptyMetadataWithEmptyLists() {
    detachedMetadataContext.put("tenant-id", List.of());
    detachedMetadataContext.put("processes", List.of());
    assertThat(detachedMetadataContext.isEffectivelyEmpty()).isTrue();
  }

  @Test
  void shouldIdentifyEmptyMetadataWithNullValues() {
    detachedMetadataContext.put("tenant-id", null);
    detachedMetadataContext.put("processes", null);
    assertThat(detachedMetadataContext.isEffectivelyEmpty()).isTrue();
  }

  @Test
  void shouldIdentifyNonEmptyMetadataWithValues() {
    detachedMetadataContext.put("tenant-id", List.of("t-1"));
    assertThat(detachedMetadataContext.isEffectivelyEmpty()).isFalse();
  }

  @Test
  void shouldIdentifyNonEmptyMetadataWithValuesAndNull() {
    detachedMetadataContext.put("tenant-id", List.of("t-1"));
    detachedMetadataContext.put("processes", null);
    assertThat(detachedMetadataContext.isEffectivelyEmpty()).isFalse();
  }

  @Test
  void shouldIdentifyNonEmptyMetadataWithValuesAndEmptyValues() {
    detachedMetadataContext.put("tenant-id", List.of("t-1"));
    detachedMetadataContext.put("processes", List.of());
    assertThat(detachedMetadataContext.isEffectivelyEmpty()).isFalse();
  }
}
