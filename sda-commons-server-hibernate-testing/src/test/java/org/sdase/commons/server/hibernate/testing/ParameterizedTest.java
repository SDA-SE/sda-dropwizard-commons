package org.sdase.commons.server.hibernate.testing;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DBUnit(url = "jdbc:h2:mem:test", driver = "org.h2.Driver", user = "sa", password = "sa")
@ExtendWith(DBUnitExtension.class)
class ParameterizedTest {

  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.arguments(0, 0), Arguments.arguments(1, 1), Arguments.arguments(2, 2));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @MethodSource("data")
  void shouldJustRunMoreThanOneTime(int given, int expected) {
    Assertions.assertThat(given).isEqualTo(expected);
  }
}
