package org.sdase.commons.server.testing.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.function.Function;

/** A utility to manipulate {@code $ref}erences in schemas. */
public class RefUtil {

  private RefUtil() {
    // utility class
  }

  /**
   * Rewrites the text value of all {@code $ref}erences in the given schema {@code node} according
   * the result of the given {@code refRewriter} function.
   *
   * <p>An example to replace all {@code $ref} pointing to {@code definitions} with refs pointing to
   * {@code components/schemas} will look like this:
   *
   * <pre>
   *   RefUtil.updateAllRefsRecursively(schemaNode, refValueTextNode -> {
   *     String ref = refValueTextNode.asText();
   *     if (ref.startsWith("#/definitions/")) {
   *       return "#/components/schemas/" + ref.substring("#/definitions/".length());
   *     } else {
   *       return ref;
   *     }
   *   });
   * </pre>
   *
   * @param node the root node of a schema that may contain {@code $ref}erences somewhere in the
   *     Json structure
   * @param refRewriter A function that receives the current value of every {@code $ref} field and
   *     returns the new value as {@code String}. If a specific {@code $ref} is not handled, the
   *     function MUST return the original {@linkplain TextNode#asText() String value} to keep it.
   */
  public static void updateAllRefsRecursively(
      JsonNode node, Function<TextNode, String> refRewriter) {
    // Copy code from org.sdase.commons.shared.asyncapi.util.RefUtil.updateAllRefsRecursively
    // to here when removing the asyncapi module.
    // So far we avoid the code duplication.

    org.sdase.commons.shared.asyncapi.util.RefUtil.updateAllRefsRecursively(node, refRewriter);
  }

  /**
   * Replaces all found {@code $ref}erence properties in the given schema {@code node} by removing
   * it from their containing {@link ObjectNode} and adding all fields of the {@link ObjectNode}
   * returned by the given {@code refRewriter}. All existing properties in the containing {@link
   * ObjectNode} remain unchanged unless they are part of the returned rewrite object.
   *
   * @param node the root node of a schema that may contain {@code $ref}erences somewhere in the
   *     Json structure
   * @param refRewriter A function that receives the current value of every {@code $ref} field and
   *     returns an {@link ObjectNode} with all fields that should be {@code ObjectNode.put(…)} to
   *     the {@link ObjectNode} containing the {@code $ref} property. If a specific {@code $ref} is
   *     not handled, the function MUST return an {@link ObjectNode} with a {@code $ref} property
   *     having the given value.
   */
  public static void mergeAllObjectsWithRefsRecursively(
      JsonNode node, Function<TextNode, ObjectNode> refRewriter) {
    // Copy code from
    // org.sdase.commons.shared.asyncapi.util.RefUtil.mergeAllObjectsWithRefsRecursively
    // to here when removing the asyncapi module.
    // So far we avoid the code duplication.

    org.sdase.commons.shared.asyncapi.util.RefUtil.mergeAllObjectsWithRefsRecursively(
        node, refRewriter);
  }
}
