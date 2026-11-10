package org.sdase.commons.shared.asyncapi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.SortedMap;
import java.util.TreeMap;

public class JsonNodeUtil {

  private JsonNodeUtil() {
    // No constructor
  }

  /**
   * @deprecated use {@code org.sdase.commons.server.testing.json.JsonNodeUtil} from
   *     sda-commons-server-testing instead
   */
  @Deprecated(forRemoval = true)
  public static void sortJsonNodeInPlace(JsonNode node) {
    if (!node.isMissingNode() && node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      SortedMap<String, JsonNode> fields = new TreeMap<>();
      objectNode.fields().forEachRemaining(e -> fields.put(e.getKey(), e.getValue()));
      objectNode.removeAll();
      fields.forEach(objectNode::set);
    }
  }
}
