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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special assertions for {@link Path} objects to check if a file matches the expected contents and
 * updates them if needed.
 *
 * <p>These assertions are helpful to check if certain files are stored in the repository (like
 * OpenAPI or AsyncApi).
 */
@SuppressWarnings("java:S2160") // no need to override equals here
public class GoldenFileAssertions extends AbstractAssert<GoldenFileAssertions, Path> {

  private static final Logger LOG = LoggerFactory.getLogger(GoldenFileAssertions.class);

  private CiUtil ciUtil = new CiUtil();

  private static final String ASSERTION_TEXT =
      "The current %s file is not up-to-date. If this "
          + "happens locally, just run the test again. The %s file is updated automatically after "
          + "running this test. If this happens in the CI, make sure that you have committed the "
          + "latest %s file!";

  /**
   * Constructor
   *
   * @param actual the path to test
   */
  private GoldenFileAssertions(Path actual) {
    super(actual, GoldenFileAssertions.class);
  }

  GoldenFileAssertions withCiUtil(CiUtil ciUtil) {
    this.ciUtil = ciUtil;
    return this;
  }

  /**
   * Creates a new instance of {@link GoldenFileAssertions} that asserts the content as text.
   *
   * @param actual the path to test
   * @return the created assertion object
   */
  public static GoldenFileAssertions assertThat(Path actual) {
    return new GoldenFileAssertions(actual);
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
   * <p>Please note that we will not update the golden version if we run in a CI pipeline such as
   * Github Actions or Jenkins.
   *
   * <p>Examples:
   *
   * <pre><code class="java">
   * Path xFile = Paths.get("openapi.yaml");
   *
   * String expected = ...; // call the service / start the generator
   *
   * GoldenFileAssertions.assertThat(xFile).hasContentAndUpdateGolden(expected);
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

      // assert normal text
      Assertions.assertThat(actual)
          .content(StandardCharsets.UTF_8)
          .as(ASSERTION_TEXT, fileName, fileName, fileName)
          .isEqualTo(expected);

    } finally {
      // eventually update the file content
      if (ciUtil.isRunningInCiPipeline()) {
        LOG.info("Not updating file {} when running in CI pipeline", actual);
      } else {
        Files.writeString(actual, expected);
      }
    }

    return this;
  }

  /**
   * Verifies that the text content of the actual {@code Path} equals the semantic of the given YAML
   * content. If not, an {@link AssertionError} is thrown, but in contrast to {@link
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
   * GoldenFileAssertions.assertThat(xFile).hasYamlContentAndUpdateGolden(expected);
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
  public GoldenFileAssertions hasYamlContentAndUpdateGolden(String expected) throws IOException {
    // check if path is not null
    isNotNull();

    // assert the file
    String fileName = actual.getFileName().toString();

    try {
      // assert if exists
      Assertions.assertThat(actual).as(ASSERTION_TEXT, fileName, fileName, fileName).exists();

      // assert YAML / JSON
      ObjectMapper objectMapper = Jackson.newObjectMapper(new YAMLFactory());
      Assertions.assertThat(objectMapper.readTree(actual.toFile()))
          .as(ASSERTION_TEXT, fileName, fileName, fileName)
          .isEqualTo(objectMapper.readTree(expected));
    } finally {
      // eventually update the file content
      if (ciUtil.isRunningInCiPipeline()) {
        LOG.info("Not updating file {} when running in CI pipeline", actual);
      } else {
        Files.writeString(actual, expected);
      }
    }

    return this;
  }
}
