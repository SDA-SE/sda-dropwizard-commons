package org.sdase.commons.server.cloudevents;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.testing.GoldenFileAssertions;
import org.sdase.commons.shared.asyncapi.AsyncApiGenerator;

class AsyncApiDocumentationTest {
  @Test
  void generateAndVerifySpec() throws IOException {
    String expected =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource("/asyncapi-template.yml"))
            .generateYaml();

    // specify where you want your file to be stored
    Path filePath = Paths.get("asyncapi.yaml");

    // check and update the file
    GoldenFileAssertions.assertThat(filePath).hasYamlContentAndUpdateGolden(expected);
  }
}
