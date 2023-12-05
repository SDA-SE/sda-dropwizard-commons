/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sdase.commons.shared.asyncapi.util.RefUtil;

/**
 * Creates Json Schema from (annotated) class files. Considered annotations depend on the
 * implementation.
 */
public interface JsonSchemaBuilder {

  /**
   * Creates <a href="https://json-schema.org/">Json Schema</a> (in version <a
   * href="https://json-schema.org/draft-07/json-schema-hypermedia.html">Draft 07</a> as <a
   * href="https://www.asyncapi.com/docs/reference/specification/v2.6.0#schemaObject">required by
   * AsyncAPI</a>) for the given {@code type}.
   *
   * @param type the java type, usually a {@link Class}, for which the Json Schema should be
   *     created.
   * @return A {@link Map} with the Json Schemas of all types needed to describe the given {@code
   *     type}. The key of the map qualifies as definition key as used in {@code
   *     /components/schemas/<key>} for the respective Json Schema. One key exactly matches the
   *     {@link Class#getSimpleName()} of the given {@code type} and refers to the Json Schema of
   *     that type. All references to other generated schemas in the returned map are qualified as
   *     {@code $ref: "#/components/messages/<map-entry-key>} when referenced.
   * @see RefUtil#updateAllRefsRecursively(JsonNode, java.util.function.Function) Adapting the
   *     references, when using the generated schemas in another documentation structure.
   */
  Map<String, JsonNode> toJsonSchema(Type type);

  /**
   * Creates <a href="https://json-schema.org/">Json Schema</a> (in version <a
   * href="https://json-schema.org/draft-07/json-schema-hypermedia.html">Draft 07</a> as <a
   * href="https://www.asyncapi.com/docs/reference/specification/v2.6.0#schemaObject">required by
   * AsyncAPI</a>) for all given {@code types}. It may happen, that schemas are generated multiple
   * times if types required to describe the given types are needed multiple times. In this case the
   * last generated schema takes precedence.
   *
   * <p>In the default implementation, conflicting schemas may be created if classes with the same
   * simple name are involved, e.g. a {@code Status} enum from different packages, when used in
   * different given types. These conflicts are not resolved unless the resulting schema is equal,
   * but an {@link IllegalStateException} is thrown.
   *
   * @param types the java types, usually {@link Class}es, for which the Json Schema should be
   *     created. Types given multiple times are only considered once.
   * @return A {@link Map} with the Json Schemas of all types needed to describe the given {@code
   *     types}. The key of the map qualifies as definition key as used in {@code
   *     /components/schemas/<key>} for the respective Json Schemas. One key exactly matches the
   *     {@link Class#getSimpleName()} of the given {@code types} and refers to the Json Schema of
   *     that type. All references to other generated schemas in the returned map are qualified as
   *     {@code $ref: "#/components/messages/<map-entry-key>} when referenced.
   * @throws IllegalStateException if conflicting schemas are produced.
   * @see RefUtil#updateAllRefsRecursively(JsonNode, java.util.function.Function) Adapting the
   *     references, when using the generated schemas in another documentation structure.
   */
  default Map<String, JsonNode> toJsonSchema(Iterable<Type> types) {
    return StreamSupport.stream(types.spliterator(), false)
        .distinct()
        .map(this::toJsonSchema)
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .distinct() // mitigates duplicate schemas transitively used in multiple input classes
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (v1, v2) -> {
                  throw new IllegalStateException(
                      "Two schema use the the same key:%n%n%s%n%n%s".formatted(v1, v2));
                },
                LinkedHashMap::new));
  }
}
