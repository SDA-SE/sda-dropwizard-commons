package org.sdase.commons.server.jackson.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.ISBN;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JerseyValidationExceptionMapperTest {

  @ParameterizedTest
  @MethodSource("testData")
  void shouldConvertToSnakeCase(String given, String expected) {
    String actual = JerseyValidationExceptionMapper.camelToUpperSnakeCase(given);
    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("realTestData")
  void differenceToDefault(String given) {
    String actual = JerseyValidationExceptionMapper.camelToUpperSnakeCase(given);
    String expected = new PropertyNamingStrategies.UpperSnakeCaseStrategy().translate(given);
    // would fail on: EAN, ISBN, URL, CNPJ, CPF, NIP, PESEL, REGON, INN
    assumeThat(actual).isEqualTo(expected);
  }

  static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of("NotNull", "NOT_NULL"),
        Arguments.of("Email", "EMAIL"),
        Arguments.of("ISBN", "I_S_B_N"), // how Guava did it, default Jackson: "ISBN"
        Arguments.of("ValidURI", "VALID_U_R_I"), // how Guava did it, default Jackson: "VALID_URI"
        Arguments.of("GreaterThan10", "GREATER_THAN10"),
        Arguments.of("GreaterThan10AndLessThan231", "GREATER_THAN10_AND_LESS_THAN231"));
  }

  static Stream<Arguments> realTestData() {
    String javaxConstraintsPackageName = NotNull.class.getPackage().getName();
    String hibernateConstraintsPackageName = ISBN.class.getPackage().getName();
    return new ClassGraph()
        .enableClassInfo().scan().getAllClasses()
            .filter(
                classInfo ->
                    classInfo.getPackageName().startsWith(javaxConstraintsPackageName)
                        || classInfo.getPackageName().startsWith(hibernateConstraintsPackageName))
            .stream()
            .map(ClassInfo::getSimpleName)
            .map(Arguments::of);
  }
}
