package org.sdase.commons.shared.asyncapi.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.shared.asyncapi.internal.JsonNodeUtil.sortJsonNodeInPlace;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.yaml.YamlUtil;

class JsonNodeUtilTest {

  @Test
  void shouldSortJsonNodeInPlace() {
    JsonNode jsonNode =
        YamlUtil.load("{\"toSort\": {\"b\": 1, \"z\": 2, \"a\": 0}}", JsonNode.class);
    JsonNode nodeToSort = jsonNode.at("/toSort");
    sortJsonNodeInPlace(nodeToSort);
    List<String> keys = new ArrayList<>();
    nodeToSort.fieldNames().forEachRemaining(keys::add);
    assertThat(keys).isSorted();
  }
}
