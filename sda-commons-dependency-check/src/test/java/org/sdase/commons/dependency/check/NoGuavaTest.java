package org.sdase.commons.dependency.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoGuavaTest {

  private static final Logger LOG = LoggerFactory.getLogger(NoGuavaTest.class);
  private static final String[] IMPORT_COM_GOOGLE = {
    "import com.google.", "import static com.google."
  };

  /**
   * This test finds usages of `com.google` in our code. We don't want to use it, because dependency
   * management of Google Guava is difficult due to multiple variants and Guava introduces breaking
   * changes from time to time.
   */
  @Test
  public void discourageUseOfGoogleCode() {
    ClassInfoList allClasses = new ClassGraph().enableClassInfo().scan().getAllClasses();
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

  private String findSource(String path) throws IOException {
    Optional<Path> sourceFile =
        Files.find(
                new File("..").toPath(), 20, (p, x) -> p.toString().endsWith("" + path + ".java"))
            .findFirst();
    if (sourceFile.isPresent()) {
      try (InputStream is = sourceFile.get().toUri().toURL().openStream()) {
        return IOUtils.toString(is, StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
