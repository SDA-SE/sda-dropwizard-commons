package org.sdase.commons.server.dropwizard.bundles.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MappableFieldTest {

  @Test
  void shouldReplaceOneKeyTwice() {
    var given = new MappableField(List.of("aMap", "<key>"), Map.class);
    var givenContext = Set.of("AMAP_foo", "AMAP_BAR");
    var actual = given.expand(givenContext);
    assertThat(actual)
        .extracting(MappableField::getJsonPathToProperty, MappableField::getContextKey)
        .containsExactlyInAnyOrder(
            tuple(List.of("aMap", "foo"), "AMAP_foo"), tuple(List.of("aMap", "BAR"), "AMAP_BAR"));
  }
}
