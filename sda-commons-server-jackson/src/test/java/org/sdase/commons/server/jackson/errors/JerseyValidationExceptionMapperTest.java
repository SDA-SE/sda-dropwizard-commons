package org.sdase.commons.server.jackson.errors;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.util.stream.Stream;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.ISBN;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JerseyValidationExceptionMapperTest {

  @ParameterizedTest
  @MethodSource("testData")
  void shouldConvertCustomDataToSnakeCase(String given, String expected) {
    String actual = JerseyValidationExceptionMapper.camelToUpperSnakeCase(given);
    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("realTestData")
  void shouldConvertValidationClassesToSnakeCase(String given) {
    String actual = JerseyValidationExceptionMapper.camelToUpperSnakeCase(given);
    String expected = new PropertyNamingStrategies.UpperSnakeCaseStrategy().translate(given);
    assertThat(actual).isEqualTo(expected);
  }

  /** Custom test data for edge case / custom testing. */
  static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of(NotNull.class.getSimpleName(), "NOT_NULL"),
        Arguments.of(Email.class.getSimpleName(), "EMAIL"),
        Arguments.of(ISBN.class.getSimpleName(), "ISBN"),
        Arguments.of("ValidURI", "VALID_URI"),
        Arguments.of("GreaterThan10", "GREATER_THAN10"),
        Arguments.of("GreaterThan10AndLessThan231", "GREATER_THAN10_AND_LESS_THAN231"));
  }

  static Stream<Arguments> realTestData() {
    String javaxConstraintsPackageName = NotNull.class.getPackage().getName();
    String hibernateConstraintsPackageName = ISBN.class.getPackage().getName();
    try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {
      return scanResult
          .getAllClasses()
          .filter(
              classInfo ->
                  classInfo.getPackageName().startsWith(javaxConstraintsPackageName)
                      || classInfo.getPackageName().startsWith(hibernateConstraintsPackageName))
          .stream()
          .map(ClassInfo::getSimpleName)
          .map(Arguments::of);
    }
  }
}
