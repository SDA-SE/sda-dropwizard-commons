package org.sdase.commons.dependency.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AvoidJavaxTest {

  private static final Logger LOG = LoggerFactory.getLogger(AvoidJavaxTest.class);

  final Set<String> ALLOWED_FILE_PARTS =
      Stream.of(
              // We have no influence on the Gradle internals. They do not influence our code.
              "/wrapper/dists/gradle-")
          .collect(Collectors.toSet());

  /**
   * This test finds and logs classes of the {@code javax.} package in the classpath. As Spring Boot
   * moved to Jakarta, such classes should not be available anymore.
   */
  @Test
  // Normalizing the file path defined in ALLOWED_FILED_PARTS for Windows failed a few times. The
  // dependencies should be OS independent, so we don't need this test on Windows.
  @DisabledOnOs(OS.WINDOWS)
  void checkForJavax() {
    Set<String> libsWithJavax = new HashSet<>();
    int notAllowedJavaxClassesCount = 0;
    try (ScanResult scanResult = new ClassGraph().enableClassInfo().scan()) {
      var allClasses = scanResult.getAllClasses();
      for (var clazz : allClasses) {
        if (clazz.getPackageName().startsWith("javax.")) {
          var location = clazz.getClasspathElementFile().toString();
          libsWithJavax.add(location);
          if (ALLOWED_FILE_PARTS.stream().noneMatch(location::contains)) {
            LOG.warn("Found {} in {}", clazz, location);
            notAllowedJavaxClassesCount++;
          }
        }
      }
    } finally {
      LOG.info("Sources of Javax: {}", libsWithJavax);
      assertThat(notAllowedJavaxClassesCount).isZero();
    }
  }
}
