package org.sdase.commons.server.morphia.internal;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

class CommandSanitizer {

  private CommandSanitizer() {
    // No instances
  }

  static BsonDocument sanitize(BsonDocument command) {
    BsonDocument sanitizedDocument = new BsonDocument();

    command.forEach(
        (key, value) -> {
          if (value.isArray() || value.isDocument()) {
            sanitizedDocument.put(key, sanitizeValue(value));
          } else {
            // The top most document doesn't contain privacy relevant data, therefore we don't
            // sanitize here.
            sanitizedDocument.put(key, value);
          }
        });

    return sanitizedDocument;
  }

  private static BsonValue sanitizeValue(BsonValue value) {
    if (value.isDocument()) {
      BsonDocument result = new BsonDocument();
      value.asDocument().forEach((key, v) -> result.put(key, sanitizeValue(v)));
      return result;
    } else if (value.isArray()) {
      BsonArray result = new BsonArray();
      value.asArray().forEach(v -> result.add(sanitizeValue(v)));
      return result;
    } else {
      return new BsonString("…");
    }
  }
}
