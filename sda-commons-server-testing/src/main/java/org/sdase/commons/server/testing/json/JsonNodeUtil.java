package org.sdase.commons.server.testing.json;

import java.util.SortedMap;
import java.util.TreeMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** A utility to sort the fields of a Jackson 3 object node in place. */
public class JsonNodeUtil {
  private JsonNodeUtil() {
    // No constructor
  }

  public static void sortJsonNodeInPlace(JsonNode node) {
    if (!node.isMissingNode() && node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      SortedMap<String, JsonNode> fields = new TreeMap<>();
      objectNode.properties().forEach(e -> fields.put(e.getKey(), e.getValue()));
      objectNode.removeAll();
      fields.forEach(objectNode::set);
    }
  }
}
