package org.sdase.commons.server.testing.json;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeUtil {

  private JsonNodeUtil() {
    // No constructor
  }

  /**
   * Sorts the fields in the given {@link JsonNode} if it is an {@link
   * com.fasterxml.jackson.databind.node.ObjectNode}. The given node will be modified.
   *
   * @param node the node which fields are sorted when it is an {@link
   *     com.fasterxml.jackson.databind.node.ObjectNode}
   */
  public static void sortJsonNodeInPlace(JsonNode node) {
    // Copy code from org.sdase.commons.shared.asyncapi.util.JsonNodeUtil.sortJsonNodeInPlace
    // to here when removing the asyncapi module.
    // So far we avoid the code duplication.
    org.sdase.commons.shared.asyncapi.util.JsonNodeUtil.sortJsonNodeInPlace(node);
  }
}
