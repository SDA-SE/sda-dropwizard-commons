package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoldenFileAssertionsTest {

  @TempDir private Path tempDir;

  private Path tempFile;

  @BeforeEach
  void setUp() {
    tempFile = tempDir.resolve("file");
  }

  @Test
  void textShouldNotThrowOnCorrectFileContent() throws IOException {
    // create file with expected-content
    Files.write(tempFile, "expected-content".getBytes());

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile)
                    .hasContentAndUpdateGolden("expected-content"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile).hasContent("expected-content");
  }

  @Test
  void textShouldNotThrowOnCorrectFileContentWithSpecialCharacters() throws IOException {
    // create file with expected-content
    Files.write(tempFile, "expected-content-\u00f6".getBytes(StandardCharsets.UTF_8));

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile)
                    .hasContentAndUpdateGolden("expected-content-ö"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile).hasBinaryContent("expected-content-ö".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void textShouldThrowOnInvalidFileContent() throws IOException {
    // create file with unexpected-content
    Files.write(tempFile, "unexpected-content".getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(tempFile);
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden("expected-content"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            tempFile.getFileName().toString());

    // content should now be expected-content
    assertThat(tempFile).hasContent("expected-content");
  }

  @Test
  void textShouldThrowOnMissingFile() {
    // use a file that does not yet exist
    Path path = tempDir.resolve("non-existing-file.yaml");

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(path);
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden("expected-content"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());

    // content should now be expected-content
    assertThat(path).exists().hasContent("expected-content");
  }

  @Test
  void yamlShouldNotThrowOnCorrectFileContent() throws IOException {
    // create file with expected-content
    Files.write(tempFile, "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: b".getBytes());

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile)
                    .hasYamlContentAndUpdateGolden(
                        "key0: v\nkey2:\n  nested2: b\n  nested1: a\nkey1: w"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile).hasContent("key0: v\nkey2:\n  nested2: b\n  nested1: a\nkey1: w");
  }

  @Test
  void yamlShouldNotThrowOnCorrectFileContentWithSpecialCharacters() throws IOException {
    // create file with expected-content
    Files.write(
        tempFile,
        "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: \u00f6"
            .getBytes(StandardCharsets.UTF_8));

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile)
                    .hasYamlContentAndUpdateGolden(
                        "key0: v\nkey2:\n  nested2: ö\n  nested1: a\nkey1: w"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile)
        .hasBinaryContent(
            "key0: v\nkey2:\n  nested2: \u00f6\n  nested1: a\nkey1: w"
                .getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void yamlShouldThrowOnInvalidFileContent() throws IOException {
    // create file with unexpected-content
    Files.write(tempFile, "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: b".getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(tempFile);
    assertThatThrownBy(
            () ->
                goldenFileAssertions.hasYamlContentAndUpdateGolden(
                    "key0: w\nkey1: x\nkey2:\n  nested1: b\n  nested2: c"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            tempFile.getFileName().toString());

    // content should now be expected-content
    assertThat(tempFile).hasContent("key0: w\nkey1: x\nkey2:\n  nested1: b\n  nested2: c");
  }

  @Test
  void jsonShouldNotThrowOnCorrectFileContent() throws IOException {
    // create file with expected-content
    Files.write(
        tempFile,
        "{\"key0\": \"v\",\"key1\": \"w\",\"key2\":{\"nested1\":\"a\",\"nested2\": \"b\"}}"
            .getBytes());

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile)
                    .hasYamlContentAndUpdateGolden(
                        "{\"key0\": \"v\",\"key2\":{\"nested2\":\"b\",\"nested1\": \"a\"},\"key1\": \"w\"}"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile)
        .hasContent(
            "{\"key0\": \"v\",\"key2\":{\"nested2\":\"b\",\"nested1\": \"a\"},\"key1\": \"w\"}");
  }

  @Test
  void jsonShouldThrowOnInvalidFileContent() throws IOException {
    // create file with unexpected-content
    Files.write(
        tempFile,
        ("{\"key0\": \"v\",\"key1\": \"w\",\"key2\":{\"nested1\":\"a\",\"nested2\": \"b\"}}")
            .getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(tempFile);
    assertThatThrownBy(
            () ->
                goldenFileAssertions.hasYamlContentAndUpdateGolden(
                    "{\"key0\": \"2\",\"key1\": \"x\",\"key2\":{\"nested1\":\"b\",\"nested2\": \"c\"}}"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            tempFile.getFileName().toString());

    // content should now be expected-content
    assertThat(tempFile)
        .hasContent(
            "{\"key0\": \"2\",\"key1\": \"x\",\"key2\":{\"nested1\":\"b\",\"nested2\": \"c\"}}");
  }

  @Test
  void yamlShouldThrowOnMissingFile() {
    // use a file that does not yet exist
    Path path = tempDir.resolve("non-existing-file.yaml");

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(path);
    assertThatThrownBy(() -> goldenFileAssertions.hasYamlContentAndUpdateGolden("expected-content"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());

    // content should now be expected-content
    assertThat(path).exists().hasContent("expected-content");
  }
}
