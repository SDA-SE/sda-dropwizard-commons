package org.sdase.commons.dependency.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DuplicateClassesTest {

  private static final List<Pattern> ignorePatterns =
      List.of(
          // There only seems to be one very old release of aopalliance that HK2 and Spring
          // repackaged into their own artifacts. Assumption is that the included versions are
          // identical and the duplication is not an issue.
          Pattern.compile("org/aopalliance.*"),
          // There is jna and jna-jpms; The second is a version of the lib with extra platform
          // related dependencies. Flapdoodle includes both libs. The duplicate classes should be
          // identical.
          Pattern.compile("com/sun/jna.*"));

  private static final Logger LOG = LoggerFactory.getLogger(DuplicateClassesTest.class);

  /**
   * This test finds and logs duplicate classes in the classpath. Such duplicates appear for example
   * when some libraries repackage standard functionality or APIs like javax.* or jakarta.* or when
   * providers change their Maven GAV without changing the internal package structure. In both cases
   * the dependency management can't identify the duplication.
   *
   * <p>When this test is not ignored any more by the assumption, the assumption can be turned into
   * an assertion.
   *
   * <p>This approach of finding duplicates is inspired by <a
   * href="https://stackoverflow.com/a/52639079">Stackoverflow</a>
   */
  @Test
  void checkForDuplicateClasses() {
    int numberOfDuplicates = 0;
    try (ScanResult scanResult = new ClassGraph().scan()) {
      ResourceList allResourcesInClasspath = scanResult.getAllResources();
      ResourceList classFilesInClasspath =
          allResourcesInClasspath
              .filter(resource -> !resource.getURL().toString().contains("/.gradle/wrapper/"))
              .classFilesOnly();
      for (Map.Entry<String, ResourceList> duplicate : classFilesInClasspath.findDuplicatePaths()) {
        if ("module-info.class".equals(duplicate.getKey())) {
          continue;
        }
        if (ignorePatterns.stream().anyMatch(p -> p.matcher(duplicate.getKey()).matches())) {
          continue;
        }
        LOG.warn("Class files path: {}", duplicate.getKey()); // Classfile path
        numberOfDuplicates++;
        for (Resource res : duplicate.getValue()) {
          LOG.warn(" -> {}", res.getURL()); // Resource URL, showing classpath element
        }
      }
      LOG.warn("Found {} duplicates.", numberOfDuplicates);
      assertThat(numberOfDuplicates)
          .describedAs("expecting no duplicate classes but found %s", numberOfDuplicates)
          .isZero();
    }
  }
}
