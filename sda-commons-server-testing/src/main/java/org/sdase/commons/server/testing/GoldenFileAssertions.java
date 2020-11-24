package org.sdase.commons.server.testing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.jackson.Jackson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

/**
 * Special assertions for {@link Path} objects to check if a file matches the expected contents and
 * updates them if needed.
 *
 * <p>These assertions are helpful to check if certain files are stored in the repository (like
 * OpenAPI or AsyncApi).
 */
public class GoldenFileAssertions extends AbstractAssert<GoldenFileAssertions, Path> {
  private static final String ASSERTION_TEXT =
      "The current %s file is not up-to-date. If this "
          + "happens locally, just run the test again. The %s file is updated automatically after "
          + "running this test. If this happens in the CI, make sure that you have committed the "
          + "latest %s file!";

  private final boolean asYaml;

  /**
   * Constructor
   *
   * @param actual the path to test
   */
  private GoldenFileAssertions(Path actual, boolean asYaml) {
    super(actual, GoldenFileAssertions.class);
    this.asYaml = asYaml;
  }

  /**
   * Creates a new instance of {@link GoldenFileAssertions} that asserts the content as text.
   *
   * @param actual the path to test
   * @return the created assertion object
   */
  public static GoldenFileAssertions assertThat(Path actual) {
    return new GoldenFileAssertions(actual, false);
  }

  /**
   * Creates a new instance of {@link GoldenFileAssertions} that asserts the content as YAML. This
   * means that it ignores the order of properties in a file.
   *
   * @param actual the path to test
   * @return the created assertion object
   */
  public static GoldenFileAssertions assertThatYaml(Path actual) {
    return new GoldenFileAssertions(actual, true);
  }

  /**
   * Verifies that the text content of the actual {@code Path} is <b>exactly</b> equal to the given
   * one. If not, an {@link AssertionError} is thrown, but in contrast to {@link
   * org.assertj.core.api.PathAssert#hasContent(String)} the file is updated with the expected value
   * so the next assert succeeds.
   *
   * <p>Use this assertion if you want to conveniently store the latest copy of a file in your
   * repository, and let the CI fail if an update has not been committed.
   *
   * <p>Examples:
   *
   * <pre><code class="java">
   * Path xFile = Paths.get("openapi.yaml");
   *
   * String expected = ...; // call the service / start the generator
   *
   * GoldenFileAssertions.assertThat(xFile).hasContentOnDisk(expected);
   * </code></pre>
   *
   * @param expected the expected text content to compare the actual {@code Path}'s content to.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given content is {@code null}.
   * @throws UncheckedIOException if an I/O error occurs.
   * @throws AssertionError if the actual {@code Path} is {@code null}.
   * @throws AssertionError if the actual {@code Path} is not a {@link Files#isReadable(Path)
   *     readable} file.
   * @throws AssertionError if the content of the actual {@code Path} is not equal to the given
   *     content.
   */
  public GoldenFileAssertions hasContentAndUpdateGolden(String expected) throws IOException {
    // check if path is not null
    isNotNull();

    // assert the file
    String fileName = actual.getFileName().toString();

    try {
      // assert if exists
      Assertions.assertThat(actual).as(ASSERTION_TEXT, fileName, fileName, fileName).exists();

      if (asYaml) {
        // assert YAML / JSON
        ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
        Assertions.assertThat(objectMapper.readTree(actual.toFile()))
            .as(ASSERTION_TEXT, fileName, fileName, fileName)
            .isEqualTo(objectMapper.readTree(expected));
      } else {
        // assert normal text
        Assertions.assertThat(actual)
            .as(ASSERTION_TEXT, fileName, fileName, fileName)
            .hasContent(expected);
      }

    } finally {
      // always update the file content
      Files.write(actual, expected.getBytes(StandardCharsets.UTF_8));
    }

    return this;
  }
}
