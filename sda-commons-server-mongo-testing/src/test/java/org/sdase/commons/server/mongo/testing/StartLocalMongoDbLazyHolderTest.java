package org.sdase.commons.server.mongo.testing;

import static org.assertj.core.api.Assertions.assertThat;

import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.store.IArtifactStore;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

class StartLocalMongoDbLazyHolderTest {

  @Test
  void shouldNotUsePrefixAndSuffixInMacOs()
      throws InvocationTargetException, IllegalAccessException {
    IArtifactStore artifactStore = StartLocalMongoDb.LazyHolder.createArtifactStore(true);

    DirectoryAndExecutableNaming extractionNaming = getExtractionNaming(artifactStore);
    DirectoryAndExecutableNaming tempNaming = getTempNaming(artifactStore);

    String executable = extractionNaming.getExecutableNaming().nameFor("prefix", "suffix");
    String temp = tempNaming.getExecutableNaming().nameFor("prefix", "suffix");

    assertThat(executable).doesNotContain("prefix").doesNotContain("suffix");
    assertThat(temp).doesNotContain("prefix").doesNotContain("suffix");
  }

  @Test
  void shouldUsePrefixAndSuffixForNonOsx()
      throws InvocationTargetException, IllegalAccessException {
    IArtifactStore artifactStore = StartLocalMongoDb.LazyHolder.createArtifactStore(false);

    DirectoryAndExecutableNaming extractionNaming = getExtractionNaming(artifactStore);
    DirectoryAndExecutableNaming tempNaming = getTempNaming(artifactStore);

    String executable = extractionNaming.getExecutableNaming().nameFor("prefix", "suffix");
    String temp = tempNaming.getExecutableNaming().nameFor("prefix", "suffix");

    assertThat(executable).contains("prefix").contains("suffix");
    assertThat(temp).contains("prefix").contains("suffix");
  }

  private DirectoryAndExecutableNaming getTempNaming(IArtifactStore artifactStore)
      throws IllegalAccessException, InvocationTargetException {
    Method temp = ReflectionUtils.getRequiredMethod(artifactStore.getClass(), "temp");
    temp.setAccessible(true);
    return (DirectoryAndExecutableNaming) temp.invoke(artifactStore);
  }

  private DirectoryAndExecutableNaming getExtractionNaming(IArtifactStore artifactStore)
      throws IllegalAccessException, InvocationTargetException {
    Method extraction = ReflectionUtils.getRequiredMethod(artifactStore.getClass(), "extraction");
    extraction.setAccessible(true);
    return (DirectoryAndExecutableNaming) extraction.invoke(artifactStore);
  }
}
