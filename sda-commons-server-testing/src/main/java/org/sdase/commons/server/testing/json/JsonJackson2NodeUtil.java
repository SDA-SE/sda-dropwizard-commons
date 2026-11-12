package org.sdase.commons.server.testing.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A utility to sort the fields of a Jackson 2 object node in place.
 *
 * @deprecated Since 10.0.0. Migrate to Jackson 3 nodes and use {@link JsonNodeUtil} instead.
 *     Jackson 2 and Jackson 3 node types are not interchangeable.
 */
@Deprecated(since = "10.0.0")
public class JsonJackson2NodeUtil {
  private JsonJackson2NodeUtil() {
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
