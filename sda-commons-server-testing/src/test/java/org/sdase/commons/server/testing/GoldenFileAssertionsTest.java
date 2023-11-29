package org.sdase.commons.server.testing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GoldenFileAssertionsTest {

  @TempDir private Path tempDir;

  private Path tempFile;
  private final CiUtil ciUtil = new CiUtil();

  @BeforeEach
  void setUp() {
    tempFile = tempDir.resolve("file");
  }

  @Test
  void textShouldNotThrowOnCorrectFileContent() throws IOException {
    // create file with expected-content
    String content = "expected-content";
    Files.write(tempFile, content.getBytes());

    // should be accepted
    assertThatCode(
            () -> GoldenFileAssertions.assertThat(tempFile).hasContentAndUpdateGolden(content))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile).hasContent(content);
  }

  @Test
  void textShouldNotThrowOnCorrectFileContentWithSpecialCharacters() throws IOException {
    // create file with expected-content
    Files.writeString(tempFile, "expected-content-\u00f6");

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile)
                    .hasContentAndUpdateGolden("expected-content-ö"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(tempFile).hasBinaryContent("expected-content-ö".getBytes(UTF_8));
  }

  @Test
  void textShouldThrowOnInvalidFileContent() throws IOException {
    // create file with unexpected-content
    String oldContent = "unexpected-content";
    Files.write(tempFile, oldContent.getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(tempFile);
    String newContent = "expected-content";
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            tempFile.getFileName().toString());

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(tempFile).hasContent(oldContent);
    } else {
      // content should now be expected-content
      assertThat(tempFile).hasContent(newContent);
    }
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

    if (ciUtil.isRunningInCiPipeline()) {
      // file should still not exist
      assertThat(path).doesNotExist();
    } else {
      // content should now be expected-content
      assertThat(path).exists().hasContent("expected-content");
    }
  }

  @Test
  void yamlShouldNotThrowOnCorrectFileContent() throws IOException {
    // create file with expected-content
    String oldContent = "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: b";
    Files.write(tempFile, oldContent.getBytes());

    // should be accepted
    String newContent = "key0: v\nkey2:\n  nested2: b\n  nested1: a\nkey1: w";
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile).hasYamlContentAndUpdateGolden(newContent))
        .doesNotThrowAnyException();

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(tempFile).hasContent(oldContent);
    } else {
      // content should be the new content
      assertThat(tempFile).hasContent(newContent);
    }
  }

  @Test
  void yamlShouldNotThrowOnCorrectFileContentWithSpecialCharacters() throws IOException {
    // create file with expected-content
    String oldContent = "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: \u00f6";
    Files.writeString(tempFile, oldContent);

    // should be accepted
    String newContent = "key0: v\nkey2:\n  nested2: ö\n  nested1: a\nkey1: w";
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile).hasYamlContentAndUpdateGolden(newContent))
        .doesNotThrowAnyException();

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(tempFile).usingCharset(UTF_8).hasContent(oldContent);
    } else {
      // content should still be expected-content
      assertThat(tempFile).usingCharset(UTF_8).hasContent(newContent);
    }
  }

  @Test
  void yamlShouldThrowOnInvalidFileContent() throws IOException {
    // create file with unexpected-content
    String oldContent = "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: b";
    Files.write(tempFile, oldContent.getBytes());

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

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(tempFile).hasContent(oldContent);
    } else {
      // content should now be expected-content
      assertThat(tempFile).hasContent("key0: w\nkey1: x\nkey2:\n  nested1: b\n  nested2: c");
    }
  }

  @Test
  void jsonShouldNotThrowOnCorrectFileContent() throws IOException {
    // create file with expected-content
    String oldContent =
        "{\"key0\": \"v\",\"key1\": \"w\",\"key2\":{\"nested1\":\"a\",\"nested2\": \"b\"}}";
    Files.write(tempFile, oldContent.getBytes());

    // should be accepted
    String newContent =
        "{\"key0\": \"v\",\"key2\":{\"nested2\":\"b\",\"nested1\": \"a\"},\"key1\": \"w\"}";
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(tempFile).hasYamlContentAndUpdateGolden(newContent))
        .doesNotThrowAnyException();

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(tempFile).hasContent(oldContent);
    } else {
      // content should still be expected-content
      assertThat(tempFile).hasContent(newContent);
    }
  }

  @Test
  void jsonShouldThrowOnInvalidFileContent() throws IOException {
    // create file with unexpected-content
    String oldContent =
        "{\"key0\": \"v\",\"key1\": \"w\",\"key2\":{\"nested1\":\"a\",\"nested2\": \"b\"}}";
    Files.write(tempFile, oldContent.getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(tempFile);
    String newContent =
        "{\"key0\": \"2\",\"key1\": \"x\",\"key2\":{\"nested1\":\"b\",\"nested2\": \"c\"}}";
    assertThatThrownBy(() -> goldenFileAssertions.hasYamlContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            tempFile.getFileName().toString());

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(tempFile).hasContent(oldContent);
    } else {
      // content should now be expected-content
      assertThat(tempFile).hasContent(newContent);
    }
  }

  @Test
  void yamlShouldThrowOnMissingFile() {
    // use a file that does not yet exist
    Path path = tempDir.resolve("non-existing-file.yaml");

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(path);
    String newContent = "expected-content";
    assertThatThrownBy(() -> goldenFileAssertions.hasYamlContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());

    if (ciUtil.isRunningInCiPipeline()) {
      assertThat(path).doesNotExist();
    } else {
      // content should now be expected-content
      assertThat(path).exists().hasContent(newContent);
    }
  }

  @Test
  void hasYamlContentAndUpdateGoldenShouldNeverUpdateContentInCi() throws IOException {
    var path = tempDir.resolve("file.yaml");
    var oldContent = "foo";
    Files.write(path, oldContent.getBytes());

    CiUtil mockCiUtil = mock(CiUtil.class);
    when(mockCiUtil.isRunningInCiPipeline()).thenReturn(true);

    var goldenFileAssertions = GoldenFileAssertions.assertThat(path).withCiUtil(mockCiUtil);
    var newContent = "bar";
    assertThatThrownBy(() -> goldenFileAssertions.hasYamlContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class);

    // content should never change
    assertThat(path).hasContent(oldContent);
  }

  @Test
  void hasContentAndUpdateGoldenShouldNeverUpdateContentInCi() throws IOException {
    var path = tempDir.resolve("file.yaml");
    var oldContent = "foo";
    Files.write(path, oldContent.getBytes());

    CiUtil mockCiUtil = mock(CiUtil.class);
    when(mockCiUtil.isRunningInCiPipeline()).thenReturn(true);

    var goldenFileAssertions = GoldenFileAssertions.assertThat(path).withCiUtil(mockCiUtil);
    var newContent = "bar";
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class);

    // content should never change
    assertThat(path).hasContent(oldContent);
  }

  @Test
  void hasYamlContentAndUpdateGoldenShouldAlwaysUpdateContent() throws IOException {
    var path = tempDir.resolve("file.yaml");
    var oldContent = "foo";
    Files.write(path, oldContent.getBytes());

    CiUtil mockCiUtil = mock(CiUtil.class);
    when(mockCiUtil.isRunningInCiPipeline()).thenReturn(false);

    var goldenFileAssertions = GoldenFileAssertions.assertThat(path).withCiUtil(mockCiUtil);
    var newContent = "bar";
    assertThatThrownBy(() -> goldenFileAssertions.hasYamlContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class);

    // content should never change
    assertThat(path).hasContent(newContent);
  }

  @Test
  void hasContentAndUpdateGoldenShouldAlwaysUpdateContent() throws IOException {
    var path = tempDir.resolve("file.yaml");
    var oldContent = "foo";
    Files.write(path, oldContent.getBytes());

    CiUtil mockCiUtil = mock(CiUtil.class);
    when(mockCiUtil.isRunningInCiPipeline()).thenReturn(false);

    var goldenFileAssertions = GoldenFileAssertions.assertThat(path).withCiUtil(mockCiUtil);
    var newContent = "bar";
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden(newContent))
        .isInstanceOf(AssertionError.class);

    // content should never change
    assertThat(path).hasContent(newContent);
  }
}
