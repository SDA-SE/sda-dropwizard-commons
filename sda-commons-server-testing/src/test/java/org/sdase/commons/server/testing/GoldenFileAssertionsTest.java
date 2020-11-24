package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GoldenFileAssertionsTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void textShouldNotThrowOnCorrectFileContent() throws IOException {
    Path path = temporaryFolder.newFile().toPath();

    // create file with expected-content
    Files.write(path, "expected-content".getBytes());

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThat(path).hasContentAndUpdateGolden("expected-content"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(path).hasContent("expected-content");
  }

  @Test
  public void textShouldThrowOnInvalidFileContent() throws IOException {
    Path path = temporaryFolder.newFile().toPath();
    // create file with unexpected-content
    Files.write(path, "unexpected-content".getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(path);
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden("expected-content"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());
    ;

    // content should now be expected-content
    assertThat(path).hasContent("expected-content");
  }

  @Test
  public void textShouldThrowOnMissingFile() {
    // use a file that does not yet exist
    Path path = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "non-existing-file.yaml");

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThat(path);
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden("expected-content"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());
    ;

    // content should now be expected-content
    assertThat(path).exists().hasContent("expected-content");
  }

  @Test
  public void yamlShouldNotThrowOnCorrectFileContent() throws IOException {
    Path path = temporaryFolder.newFile().toPath();

    // create file with expected-content
    Files.write(path, "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: b".getBytes());

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThatYaml(path)
                    .hasContentAndUpdateGolden(
                        "key0: v\nkey2:\n  nested2: b\n  nested1: a\nkey1: w"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(path).hasContent("key0: v\nkey2:\n  nested2: b\n  nested1: a\nkey1: w");
  }

  @Test
  public void yamlShouldThrowOnInvalidFileContent() throws IOException {
    Path path = temporaryFolder.newFile().toPath();
    // create file with unexpected-content
    Files.write(path, "key0: v\nkey1: w\nkey2:\n  nested1: a\n  nested2: b".getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThatYaml(path);
    assertThatThrownBy(
            () ->
                goldenFileAssertions.hasContentAndUpdateGolden(
                    "key0: w\nkey1: x\nkey2:\n  nested1: b\n  nested2: c"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());

    // content should now be expected-content
    assertThat(path).hasContent("key0: w\nkey1: x\nkey2:\n  nested1: b\n  nested2: c");
  }

  @Test
  public void jsonShouldNotThrowOnCorrectFileContent() throws IOException {
    Path path = temporaryFolder.newFile().toPath();

    // create file with expected-content
    Files.write(
        path,
        "{\"key0\": \"v\",\"key1\": \"w\",\"key2\":{\"nested1\":\"a\",\"nested2\": \"b\"}}"
            .getBytes());

    // should be accepted
    assertThatCode(
            () ->
                GoldenFileAssertions.assertThatYaml(path)
                    .hasContentAndUpdateGolden(
                        "{\"key0\": \"v\",\"key2\":{\"nested2\":\"b\",\"nested1\": \"a\"},\"key1\": \"w\"}"))
        .doesNotThrowAnyException();

    // content should still be expected-content
    assertThat(path)
        .hasContent(
            "{\"key0\": \"v\",\"key2\":{\"nested2\":\"b\",\"nested1\": \"a\"},\"key1\": \"w\"}");
  }

  @Test
  public void jsonShouldThrowOnInvalidFileContent() throws IOException {
    Path path = temporaryFolder.newFile().toPath();
    // create file with unexpected-content
    Files.write(
        path,
        ("{\"key0\": \"v\",\"key1\": \"w\",\"key2\":{\"nested1\":\"a\",\"nested2\": \"b\"}}")
            .getBytes());

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThatYaml(path);
    assertThatThrownBy(
            () ->
                goldenFileAssertions.hasContentAndUpdateGolden(
                    "{\"key0\": \"2\",\"key1\": \"x\",\"key2\":{\"nested1\":\"b\",\"nested2\": \"c\"}}"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());

    // content should now be expected-content
    assertThat(path)
        .hasContent(
            "{\"key0\": \"2\",\"key1\": \"x\",\"key2\":{\"nested1\":\"b\",\"nested2\": \"c\"}}");
  }

  @Test
  public void yamlShouldThrowOnMissingFile() {
    // use a file that does not yet exist
    Path path = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "non-existing-file.yaml");

    // should throw and update the file
    GoldenFileAssertions goldenFileAssertions = GoldenFileAssertions.assertThatYaml(path);
    assertThatThrownBy(() -> goldenFileAssertions.hasContentAndUpdateGolden("expected-content"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining(
            "The current %s file is not up-to-date. If this happens locally,",
            path.getFileName().toString());

    // content should now be expected-content
    assertThat(path).exists().hasContent("expected-content");
  }
}
