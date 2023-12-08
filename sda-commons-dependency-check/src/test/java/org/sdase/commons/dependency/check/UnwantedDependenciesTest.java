package org.sdase.commons.dependency.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UnwantedDependenciesTest {

  private static final Logger LOG = LoggerFactory.getLogger(UnwantedDependenciesTest.class);

  private static final String[] IMPORT_COM_GOOGLE = {
    "import com.google.", "import static com.google."
  };

  private static final String[] IMPORT_ORG_APCHE_HTTPCOMPONENTS = {
    "import org.apache.http.", "import static org.apache.http."
  };

  static final Set<String> ALLOWED_FILE_PARTS =
      Set.of(
          // We have no influence on the Gradle internals. They do not influence our code.
          "/wrapper/dists/gradle-");

  private static ClassInfoList allClasses;
  private static List<URI> classpathURIs;

  @BeforeAll
  static void beforeAll() {
    try (ScanResult scanResult =
        new ClassGraph()
            .enableClassInfo()
            .filterClasspathElements(path -> ALLOWED_FILE_PARTS.stream().noneMatch(path::contains))
            .scan()) {
      allClasses = scanResult.getAllClasses();
      classpathURIs = scanResult.getClasspathURIs();
    }
  }

  /**
   * This test finds usages of `com.google` in our code. We don't want to use it, because dependency
   * management of Google Guava is difficult due to multiple variants and Guava introduces breaking
   * changes from time to time.
   */
  @Test
  @DisabledOnOs(WINDOWS)
  void discourageUseOfGoogleCode() {
    ClassInfoList classFilesInClasspath =
        allClasses
            .filter(c -> c.getPackageName().startsWith("org.sdase."))
            .filter(c -> !c.getName().equals(this.getClass().getName()))
            .filter(c -> !c.getName().contains("$"));
    for (ClassInfo classInfo : classFilesInClasspath) {
      String path = classInfo.getName().replaceAll("\\.", "/");
      try {
        String source = findSource(path);
        assertThat(source)
            .describedAs("%s contains google imports", path)
            .doesNotContain(IMPORT_COM_GOOGLE);
      } catch (IOException | NullPointerException | NoSuchElementException e) {
        LOG.warn("Could not find source of {}", path);
      }
    }
  }

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
    try {
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

  /**
   * This test finds usages of `org.apache.httpcomponents` in our code. We don't want to use it,
   * because we want to use the newest version of Apache Http Client.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void checkForApacheHttpClientV4() {
    ClassInfoList classFilesInClasspath =
        allClasses
            .filter(c -> c.getPackageName().startsWith("org.apache.http"))
            .filter(c -> !c.getName().equals(this.getClass().getName()))
            .filter(c -> !c.getName().contains("$"));
    for (ClassInfo classInfo : classFilesInClasspath) {
      String path = classInfo.getName().replaceAll("\\.", "/");
      try {
        String source = findSource(path);
        assertThat(source)
            .describedAs("%s contains apache httpclient v4imports", path)
            .doesNotContain(IMPORT_ORG_APCHE_HTTPCOMPONENTS);
      } catch (IOException | NullPointerException | NoSuchElementException e) {
        LOG.warn("Could not find source of {}", path);
      }
    }
  }

  /**
   * This test finds the library `com.github.tomakehurst.wiremock` in our classpath. We don't want
   * to use it, because we want to use the official org.wiremock from Dropwizard dependencies.
   */
  @Test
  @DisabledOnOs(OS.WINDOWS)
  void checkForTomakehurstWiremock() {
    assertThat(classpathURIs)
        .noneMatch(u -> u.getPath().contains("com.github.tomakehurst.wiremock."))
        .anyMatch(u -> u.getPath().contains("org.wiremock"));
  }

  private String findSource(String path) throws IOException {
    try (Stream<Path> pathStream =
        Files.find(new File("..").toPath(), 20, (p, x) -> p.toString().endsWith(path + ".java"))) {
      Optional<Path> sourceFile = pathStream.findFirst();
      if (sourceFile.isPresent()) {
        try (InputStream is = sourceFile.get().toUri().toURL().openStream()) {
          return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
      }
    }
    return null;
  }
}
