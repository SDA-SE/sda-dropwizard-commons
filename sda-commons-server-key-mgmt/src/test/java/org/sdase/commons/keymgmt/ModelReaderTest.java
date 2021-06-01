package org.sdase.commons.keymgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import org.junit.jupiter.api.Test;

class ModelReaderTest {

  @Test
  void shouldHandleNullPathParameter() throws IOException {
    assertThat(ModelReader.parseMappingFile(null)).isNotNull().isEmpty();
    assertThat(ModelReader.parseApiKeys(null)).isNotNull().isEmpty();
  }

  @Test
  void shouldThrowExceptionOnNonExistingPath() {
    assertThrows(NoSuchFileException.class, () -> ModelReader.parseMappingFile("bla"));
    assertThrows(NoSuchFileException.class, () -> ModelReader.parseApiKeys("bla"));
  }
}
