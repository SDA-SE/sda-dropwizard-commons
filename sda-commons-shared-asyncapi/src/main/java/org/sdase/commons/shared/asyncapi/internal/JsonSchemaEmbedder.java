package org.sdase.commons.shared.asyncapi.internal;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Embeds external references in a JSON schema using a schema resolver. */
public class JsonSchemaEmbedder {

  private static final String REF = "$ref";
  private static final List<String> IGNORE_ON_EMBED = Arrays.asList("$schema", "definitions");

  private final JsonPointer embedPointer;
  private final JsonSchemaResolver jsonSchemaResolver;

  /**
   * Create a new JSON schema embedder.
   *
   * @param embedPath The JSON path where the JSON schemas should be embedded.
   * @param jsonSchemaResolver Resolver for external schema files.
   */
  public JsonSchemaEmbedder(String embedPath, JsonSchemaResolver jsonSchemaResolver) {
    this.embedPointer = JsonPointer.compile(embedPath);
    this.jsonSchemaResolver = jsonSchemaResolver;
  }

  /**
   * Resolves external references in the input JSON object.
   *
   * @param input The input JSON object containing external references.
   * @return A cloned JSON object, that contains all external references that could be resolved,
   *     embedded a the embed path.
   */
  public JsonNode resolve(JsonNode input) {
    Map<String, JsonNode> resolvedReferences = new HashMap<>();
    JsonNode output = input.deepCopy();

    resolveInPlace(output, output, output, resolvedReferences, false);

    return output;
  }

  private void resolveInPlace(
      JsonNode target,
      JsonNode root,
      JsonNode current,
      Map<String, JsonNode> resolvedReferences,
      boolean shouldEmbedInternal) {
    if (current.isArray()) {
      current.forEach(
          jsonNode ->
              resolveInPlace(target, root, jsonNode, resolvedReferences, shouldEmbedInternal));
    } else if (current.isObject()) {
      List<Entry<String, JsonNode>> fields = new ArrayList<>();
      current.fields().forEachRemaining(fields::add);

      fields.forEach(
          e -> {
            if (REF.equals(e.getKey())
                && e.getValue().isTextual()
                && shouldEmbed(e.getValue().textValue())) {

              JsonReference ref = JsonReference.parse(e.getValue().asText());

              embedReference(
                  target, root, (ObjectNode) current, resolvedReferences, ref, shouldEmbedInternal);
            } else {
              resolveInPlace(target, root, e.getValue(), resolvedReferences, shouldEmbedInternal);
            }
          });
    }
  }

  private void embedReference(
      JsonNode target,
      JsonNode root,
      ObjectNode currentObject,
      Map<String, JsonNode> resolvedReferences,
      JsonReference ref,
      boolean shouldEmbedInternal) {
    JsonNode refDocument =
        ref.isExternal()
            ? resolvedReferences.computeIfAbsent(ref.url, jsonSchemaResolver::resolve)
            : root;

    // Check if external reference
    if (refDocument != null && (ref.isExternal() || shouldEmbedInternal)) {
      JsonPointer refNamePointer =
          ref.pointer.last() == null
              ? ref.pointer.append(JsonPointer.compile("/" + replaceSpecialCharacters(ref.url)))
              : ref.pointer.last();
      String refName = refNamePointer.getMatchingProperty();

      JsonNode embedNode = ensureParentNode(target, embedPointer);
      ObjectNode embedObject = (ObjectNode) embedNode;

      // Not yet embedded?
      if (!embedObject.has(refName)) {
        JsonNode refNode = refDocument.at(ref.pointer).deepCopy();

        if (ref.pointer.last() == null) {
          // In case the root object is embedded, we have to remove the other unrelated top level
          // fields like $schema or definitions
          ObjectNode refObject = (ObjectNode) refNode;
          IGNORE_ON_EMBED.forEach(refObject::remove);
        }

        // Make sure that all internal references are also recursively embedded
        resolveInPlace(target, refDocument, refNode, resolvedReferences, true);

        embedObject.set(refName, refNode);
      }

      // Rewrite reference to embedded location
      currentObject.put(REF, new JsonReference(embedPointer.append(refNamePointer)).toString());
    }
  }

  private JsonNode ensureParentNode(JsonNode target, JsonPointer pointer) {
    JsonNode node = target.at(pointer);

    if (node.isMissingNode()) {
      ObjectNode parentNode = (ObjectNode) ensureParentNode(target, pointer.head());
      parentNode.set(pointer.last().getMatchingProperty(), JsonNodeFactory.instance.objectNode());
      node = target.at(embedPointer);
    }

    return node;
  }

  private String replaceSpecialCharacters(String value) {
    return value.replaceAll("[^a-zA-Z0-9]", "");
  }

  private boolean shouldEmbed(String value) {
    return !value.startsWith("https://");
  }
}
