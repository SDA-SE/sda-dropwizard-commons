package org.sdase.commons.server.dropwizard.bundles.scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

  @Test
  void shouldExpandArrayIndex() {
    var given =
        new MappableField(
            List.of("auth", "keyloaderClient", "proxy", "nonProxyHosts", "<index>"), List.class);
    var givenContext = Set.of("AUTH_KEYLOADERCLIENT_PROXY_NONPROXYHOSTS_0");
    var actual = given.expand(givenContext);
    assertThat(actual)
        .extracting(MappableField::getJsonPathToProperty, MappableField::getContextKey)
        .containsExactlyInAnyOrder(
            tuple(
                List.of("auth", "keyloaderClient", "proxy", "nonProxyHosts", "0"),
                "AUTH_KEYLOADERCLIENT_PROXY_NONPROXYHOSTS_0"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldEvaluateEqualsForCoverage(
      MappableField givenOne, MappableField givenTwo, boolean expectedEquals) {
    var actual = givenOne.equals(givenTwo);
    assertThat(actual).isEqualTo(expectedEquals);
    assumeThat(givenTwo).isNotNull();
    if (expectedEquals) {
      assertThat(givenOne).hasSameHashCodeAs(givenTwo);
    } else {
      assertThat(givenOne).doesNotHaveSameHashCodeAs(givenTwo);
    }
  }

  static Stream<Arguments> shouldEvaluateEqualsForCoverage() {
    return Stream.of(
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo", "bar"), String.class),
            true),
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo", "bar"), Integer.class),
            false),
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo"), String.class),
            false),
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo", "foo"), String.class),
            false),
        of(new MappableField(List.of("foo", "bar"), String.class), null, false));
  }

  @ParameterizedTest
  @MethodSource
  void shouldCompare(MappableField givenOne, MappableField givenTwo, int expectedCompare) {
    var actual = givenOne.compareTo(givenTwo);
    assertThat(Integer.signum(actual)).isEqualTo(Integer.signum(expectedCompare));
  }

  static Stream<Arguments> shouldCompare() {
    return Stream.of(
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo"), String.class),
            1),
        of(
            new MappableField(List.of("foo"), String.class),
            new MappableField(List.of("foo", "bar"), String.class),
            -1),
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo", "bar"), String.class),
            0),
        of(
            new MappableField(List.of("foo", "bar"), Integer.class),
            new MappableField(List.of("foo", "bar"), String.class),
            0),
        of(
            new MappableField(List.of("foo", "bar"), String.class),
            new MappableField(List.of("foo", "bar"), Integer.class),
            0),
        of(
            new MappableField(List.of("foo", "10", "bar"), String.class),
            new MappableField(List.of("foo", "3", "bar"), String.class),
            1),
        of(
            new MappableField(List.of("foo", "3", "bar"), String.class),
            new MappableField(List.of("foo", "10", "bar"), String.class),
            -1),
        of(
            new MappableField(List.of("foo", "10"), String.class),
            new MappableField(List.of("foo", "3", "bar"), String.class),
            1),
        of(
            new MappableField(List.of("foo", "3", "bar"), String.class),
            new MappableField(List.of("foo", "10"), String.class),
            -1));
  }

  @ParameterizedTest
  @MethodSource
  void shouldDocumentArrays(MappableField given) {
    var actual = given.getPropertyTypeDescription();
    assertThat(actual).isEqualTo("Array");
  }

  static Stream<Arguments> shouldDocumentArrays() {
    var fieldsWithStrings =
        new JacksonTypeScanner(new ObjectMapper(), JacksonTypeScanner.DROPWIZARD_PLAIN_TYPES)
            .scan(TestTypeHolderWithStrings.class);
    assertThat(fieldsWithStrings).hasSize(3);
    return fieldsWithStrings.stream().map(Arguments::of);
  }

  @SuppressWarnings("unused")
  static class TestTypeHolderWithStrings {
    private List<String> stringsInList;
    private Set<String> stringsInSet;
    private String[] stringsInArray;

    public List<String> getStringsInList() {
      return stringsInList;
    }

    public TestTypeHolderWithStrings setStringsInList(List<String> stringsInList) {
      this.stringsInList = stringsInList;
      return this;
    }

    public Set<String> getStringsInSet() {
      return stringsInSet;
    }

    public TestTypeHolderWithStrings setStringsInSet(Set<String> stringsInSet) {
      this.stringsInSet = stringsInSet;
      return this;
    }

    public String[] getStringsInArray() {
      return stringsInArray;
    }

    public TestTypeHolderWithStrings setStringsInArray(String[] stringsInArray) {
      this.stringsInArray = stringsInArray;
      return this;
    }
  }
}
