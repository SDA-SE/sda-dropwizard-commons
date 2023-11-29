package org.sdase.commons.server.dropwizard.bundles.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.dropwizard.bundles.configuration.ConfigurationRuntimeContext.AggregateConfigurationRuntimeContext;

class AggregateConfigurationRuntimeContextTest {

  @Test
  void shouldGetValuesFromMostImportantDelegate() {
    var givenContext =
        new AggregateConfigurationRuntimeContext(
            new MapTestContext(Map.of("KEY_A", "value a1")),
            new MapTestContext(Map.of("KEY_A", "value a2", "KEY_B", "value b2")),
            new MapTestContext(
                Map.of("KEY_A", "value a3", "KEY_B", "value b3", "KEY_C", "value c3")));
    assertThat(givenContext.getValue("KEY_A")).isEqualTo("value a1");
    assertThat(givenContext.getValue("KEY_B")).isEqualTo("value b2");
    assertThat(givenContext.getValue("KEY_C")).isEqualTo("value c3");
  }

  @Test
  void shouldIgnoreNullValues() {
    Map<String, String> mapWithNulls = new HashMap<>();
    mapWithNulls.put("KEY_A", null);
    var givenContext =
        new AggregateConfigurationRuntimeContext(
            new MapTestContext(mapWithNulls), new MapTestContext(Map.of("KEY_A", "value a2")));
    assertThat(givenContext.getValue("KEY_A")).isEqualTo("value a2");
  }

  @Test
  void shouldMergeAllKeys() {
    var givenContext =
        new AggregateConfigurationRuntimeContext(
            new MapTestContext(Map.of("KEY_A", "value a1")),
            new MapTestContext(Map.of("KEY_A", "value a2", "KEY_B", "value b2")),
            new MapTestContext(
                Map.of("KEY_A", "value a3", "KEY_B", "value b3", "KEY_C", "value c3")));
    assertThat(givenContext.getDefinedKeys()).containsExactlyInAnyOrder("KEY_A", "KEY_B", "KEY_C");
  }

  @Test
  void shouldNotCacheValues() {
    Map<String, String> givenData = new HashMap<>();
    var givenContext = new AggregateConfigurationRuntimeContext(new MapTestContext(givenData));
    givenData.put("KEY_A", "first value");
    assertThat(givenContext.getValue("KEY_A")).isEqualTo("first value");
    givenData.put("KEY_A", "second value");
    assertThat(givenContext.getValue("KEY_A")).isEqualTo("second value");
  }

  @Test
  void shouldNotCacheKeys() {
    Map<String, String> givenData = new HashMap<>();
    var givenContext = new AggregateConfigurationRuntimeContext(new MapTestContext(givenData));
    givenData.put("KEY_A", "first value");
    assertThat(givenContext.getDefinedKeys()).containsExactlyInAnyOrder("KEY_A");
    givenData.put("KEY_B", "second value");
    assertThat(givenContext.getDefinedKeys()).containsExactlyInAnyOrder("KEY_A", "KEY_B");
  }

  record MapTestContext(Map<String, String> delegate) implements ConfigurationRuntimeContext {

    @Override
    public Set<String> getDefinedKeys() {
      return delegate.keySet();
    }

    @Override
    public String getValue(String key) {
      return delegate.get(key);
    }
  }
}
