package org.sdase.commons.server.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.openapi.OpenApiFileHelper.normalizeOpenApiYaml;
import static org.sdase.commons.server.openapi.OpenApiFileHelper.normalizeOpenApiYamlAndReplaceVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;
import org.sdase.commons.server.testing.GoldenFileAssertions;

class OpenApiGeneratorGenerateOpenApiTest {

  @Test
  void shouldGenerateDocumentationWithoutRestCallYaml() throws Exception {
    var bundle =
        OpenApiBundle.builder()
            .addResourcePackage(OpenApiBundleTestApp.class.getPackageName())
            .build();
    var openapi = bundle.generateOpenApiAsYaml();
    assertThat(openapi).isNotNull();

    Path expectedPath = Paths.get("src/test/resources/expected-openapi.yaml");
    String actual = new String(Files.readAllBytes(expectedPath));

    GoldenFileAssertions.assertThat(expectedPath)
        .hasYamlContentAndUpdateGolden(normalizeOpenApiYaml(openapi));
    GoldenFileAssertions.assertThat(expectedPath)
        .hasYamlContentAndUpdateGolden(normalizeOpenApiYamlAndReplaceVersion(openapi, actual));
  }

  @Test
  void shouldGenerateDocumentationWithoutRestCallJson() throws Exception {
    var bundle =
        OpenApiBundle.builder()
            .addResourcePackage(OpenApiBundleTestApp.class.getPackageName())
            .build();
    var openapi = bundle.generateOpenApiAsJson();
    assertThat(openapi).isNotNull();

    Path expectedPath = Paths.get("src/test/resources/expected-openapi.json");
    GoldenFileAssertions.assertThat(expectedPath).hasContentAndUpdateGolden(openapi);
  }
}
