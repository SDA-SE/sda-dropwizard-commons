package org.sdase.commons.shared.asyncapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.junit.Test;
import org.sdase.commons.shared.asyncapi.models.BaseEvent;
import org.sdase.commons.shared.yaml.YamlUtil;

public class AsyncApiGeneratorTest {

  @Test
  public void shouldGenerateAsyncApi() throws IOException, URISyntaxException {
    String actual =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource("/asyncapi_template.yaml"))
            .withSchema("./schema.json", BaseEvent.class)
            .generateYaml();
    String expected = TestUtil.readResource("/asyncapi_expected.yaml");

    Map<String, Object> expectedJson =
        YamlUtil.load(expected, new TypeReference<Map<String, Object>>() {});
    Map<String, Object> actualJson =
        YamlUtil.load(actual, new TypeReference<Map<String, Object>>() {});

    assertThat(actualJson).isEqualToComparingFieldByFieldRecursively(expectedJson);
  }
}
