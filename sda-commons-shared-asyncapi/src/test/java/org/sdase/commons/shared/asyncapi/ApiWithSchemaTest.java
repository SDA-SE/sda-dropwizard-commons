package org.sdase.commons.shared.asyncapi;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.testing.GoldenFileAssertions;

class ApiWithSchemaTest {

  @Test
  void generateAndVerifySpec() throws IOException {
    // get template
    var template = getClass().getResource("/demo/template_with_schema.yaml");
    // generate AsyncAPI yaml
    String expected = AsyncApiGenerator.builder().withAsyncApiBase(template).generateYaml();
    // specify where to store the result, e.g. Path.of("asyncapi.yaml") for the project root.
    Path filePath = Paths.get("asyncapi-schema.yaml");
    // check and update the file
    GoldenFileAssertions.assertThat(filePath).hasYamlContentAndUpdateGolden(expected);
  }
}
