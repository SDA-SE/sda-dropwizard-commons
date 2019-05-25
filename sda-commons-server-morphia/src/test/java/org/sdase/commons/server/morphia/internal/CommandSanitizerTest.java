package org.sdase.commons.server.morphia.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.bson.BsonDocument;
import org.junit.Test;

public class CommandSanitizerTest {

  @Test
  public void shouldSanitizeCreateIndexesCommand() {
    BsonDocument command =
        BsonDocument.parse(
            "{\"createIndexes\": \"people\", \"indexes\": [{\"key\": {\"name\": 1}, \"name\": \"name_1\", \"ns\": \"testPeople.people\"}], \"$db\": \"testPeople\", \"$readPreference\": {\"mode\": \"primaryPreferred\"}}");
    BsonDocument sanitizedCommand = CommandSanitizer.sanitize(command);

    assertThat(sanitizedCommand.toJson())
        .isEqualTo(
            "{\"createIndexes\": \"people\", \"indexes\": [{\"key\": {\"name\": \"…\"}, \"name\": \"…\", \"ns\": \"…\"}], \"$db\": \"testPeople\", \"$readPreference\": {\"mode\": \"…\"}}");
  }

  @Test
  public void shouldSanitizeInsertCommand() {
    BsonDocument command =
        BsonDocument.parse(
            "{\"insert\": \"people\", \"ordered\": true, \"$db\": \"testPeople\", \"documents\": [{\"_id\": {\"$oid\": \"5e355afc6b7bb542544adf68\"}, \"className\": \"org.sdase.commons.server.morphia.test.model.Person\", \"name\": \"Max\", \"age\": 18}]}");
    BsonDocument sanitizedCommand = CommandSanitizer.sanitize(command);

    assertThat(sanitizedCommand.toJson())
        .isEqualTo(
            "{\"insert\": \"people\", \"ordered\": true, \"$db\": \"testPeople\", \"documents\": [{\"_id\": \"…\", \"className\": \"…\", \"name\": \"…\", \"age\": \"…\"}]}");
  }
}
