package org.sdase.commons.server.testing.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

class JsonNodeUtilTest {

  @Test
  void shouldSortJsonNodeInPlace() {

    String sortString = "{\"toSort\": {\"b\": 1, \"z\": 2, \"a\": 0}}";
    JsonNode jsonNode = YAMLMapper.builder().build().readTree(sortString);
    JsonNode nodeToSort = jsonNode.at("/toSort");
    JsonNodeUtil.sortJsonNodeInPlace(nodeToSort);
    List<String> keys = new ArrayList<>(nodeToSort.propertyNames());
    assertThat(keys).isSorted();
  }
}
