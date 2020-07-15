package org.sdase.commons.dependency.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateClassesTest {

  /**
   * Number of duplicates seen in the GitHub action build. This number may be different in other
   * environments for unknown reasons.
   */
  private static final int LAST_SEEN_NUMBER_OF_DUPLICATES = 34;

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
  public void checkForDuplicateClasses() {
    int numberOfDuplicates = 0;
    ResourceList allResourcesInClasspath = new ClassGraph().scan().getAllResources();
    ResourceList classFilesInClasspath =
        allResourcesInClasspath
            .filter(resource -> !resource.getURL().toString().contains("/.gradle/wrapper/"))
            .classFilesOnly();
    for (Map.Entry<String, ResourceList> duplicate : classFilesInClasspath.findDuplicatePaths()) {
      if ("module-info.class".equals(duplicate.getKey())) {
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
        .describedAs(
            "already saw only %s duplicate classes but now there are %s",
            LAST_SEEN_NUMBER_OF_DUPLICATES, numberOfDuplicates)
        .isLessThanOrEqualTo(LAST_SEEN_NUMBER_OF_DUPLICATES);
    assumeThat(numberOfDuplicates)
        .describedAs("expecting no duplicate classes but found %s", numberOfDuplicates)
        .isZero();
  }
}
