package org.sdase.commons.server.morphia.codec;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

public class LocalDateCodec implements Codec<LocalDate> {

  // we use this for parsing to be backward compatible with the default
  private static final DateTimeFormatter ISO_LOCAL_DATE_WITH_OPTIONAL_TIME =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .append(ISO_LOCAL_DATE)
          .optionalStart()
          .appendLiteral('T')
          .append(ISO_LOCAL_TIME)
          .optionalEnd()
          .optionalStart()
          .appendOffsetId()
          .toFormatter();


  @Override
  public LocalDate decode(BsonReader reader, DecoderContext decoderContext) {
    var fromDBObject = reader.readString();
    if (fromDBObject == null) {
      return null;
    }

    try {
      return LocalDate.parse(fromDBObject, ISO_LOCAL_DATE_WITH_OPTIONAL_TIME);
    }
    catch (DateTimeParseException e) {
      // If this exception arises for java.util.Date, it is most likely that the collection has been
      // set up with an older version of SDA Commons which is not compatible with this converter.
      // The LocalDateConverter causing this breaking change has been introduced by PR #185. It should
      // help to initialize the MorphiaBundle 'withoutLocalDateConverter()' if upgrading the content
      // of the database is not an option.
      throw new IllegalArgumentException(
          String.format("Cannot decode object: %s", fromDBObject));
    }
  }

  @Override
  public void encode(BsonWriter writer, LocalDate value, EncoderContext encoderContext) {
    if (value == null) {
      writer.writeNull();
    }

    writer.writeString(value.format(ISO_LOCAL_DATE));
  }

  @Override
  public Class<LocalDate> getEncoderClass() {
    return LocalDate.class;
  }
}
