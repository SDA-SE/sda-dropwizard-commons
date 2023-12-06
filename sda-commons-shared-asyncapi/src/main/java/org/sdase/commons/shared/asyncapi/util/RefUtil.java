package org.sdase.commons.shared.asyncapi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.function.Function;

/** A utility to manipulate {@code $ref}erences in schemas. */
public class RefUtil {

  private static final String REF_KEY = "$ref";

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
    // only container nodes can contain properties (directly or in nested structures)
    if (node instanceof ContainerNode<?> containerNode) {
      mergeAllObjectsWithRefsRecursively(
          containerNode,
          textNode -> containerNode.objectNode().put(REF_KEY, refRewriter.apply(textNode)));
    }
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
    if (node instanceof ObjectNode objectNode) {
      ObjectNode iteratorNode = objectNode.deepCopy(); // avoid concurrent modification
      iteratorNode
          .fieldNames()
          .forEachRemaining(
              fieldName -> {
                JsonNode fieldValueNode = objectNode.get(fieldName);
                if (fieldName.equals(REF_KEY) && fieldValueNode instanceof TextNode ref) {
                  var replacementForObjectWithRef = refRewriter.apply(ref);
                  if (replacementForObjectWithRef != null) {
                    objectNode.remove(REF_KEY);
                    objectNode.setAll(replacementForObjectWithRef);
                  }
                } else if (fieldValueNode instanceof ArrayNode arrayNode) {
                  arrayNode.forEach(item -> mergeAllObjectsWithRefsRecursively(item, refRewriter));
                } else {
                  mergeAllObjectsWithRefsRecursively(fieldValueNode, refRewriter);
                }
              });
    } else if (node instanceof ArrayNode arrayNode) {
      arrayNode.forEach(item -> mergeAllObjectsWithRefsRecursively(item, refRewriter));
    }
  }
}
