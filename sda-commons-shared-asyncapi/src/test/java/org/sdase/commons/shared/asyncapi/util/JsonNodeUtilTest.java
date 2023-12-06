package org.sdase.commons.shared.asyncapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonNodeUtilTest {

  @Test
  void shouldSortJsonNodeInPlace() throws JsonProcessingException {

    String sortString = "{\"toSort\": {\"b\": 1, \"z\": 2, \"a\": 0}}";
    JsonNode jsonNode = YAMLMapper.builder().build().readTree(sortString);
    JsonNode nodeToSort = jsonNode.at("/toSort");
    JsonNodeUtil.sortJsonNodeInPlace(nodeToSort);
    List<String> keys = new ArrayList<>();
    nodeToSort.fieldNames().forEachRemaining(keys::add);
    assertThat(keys).isSorted();
  }
}
