package org.sdase.commons.shared.asyncapi.internal;

import com.fasterxml.jackson.core.JsonPointer;

/** Describes a $ref reference in a JSON/YAML document. */
class JsonReference {

  /** The url of the referenced external file if available or null in case of internal links. */
  final String url;
  /** The pointer to the node in the referenced file. */
  final JsonPointer pointer;

  /**
   * Create a new internal reference.
   *
   * @param pointer The pointer in the current document.
   */
  public JsonReference(JsonPointer pointer) {
    this.url = null;
    this.pointer = pointer;
  }

  /**
   * Creates a new reference with an url and a pointer.
   *
   * @param url The url of the document to reference.
   * @param pointer The pointer in the document referenced by url, or the current document if url is
   *     null.
   */
  public JsonReference(String url, JsonPointer pointer) {
    this.url = url;
    this.pointer = pointer;
  }

  /**
   * True, if the reference points to an external node.
   *
   * @return Whether the reference points to an external node.
   */
  public boolean isExternal() {
    return url != null;
  }

  @Override
  public String toString() {
    if (isExternal()) {
      return url + "#" + pointer;
    } else {
      return "#" + pointer;
    }
  }

  /**
   * Parse a reference in the format example.com#/path/to/node
   *
   * @param ref The reference to parse
   * @return The parsed reference.
   */
  static JsonReference parse(String ref) {
    int urlEndIndex = ref.indexOf('#');

    if (urlEndIndex < 0) {
      throw new IllegalArgumentException("Invalid reference format: " + ref);
    }

    String url = urlEndIndex == 0 ? null : ref.substring(0, urlEndIndex);
    String pointerText = ref.substring(urlEndIndex + 1);
    JsonPointer pointer = JsonPointer.compile(pointerText);

    return new JsonReference(url, pointer);
  }
}
