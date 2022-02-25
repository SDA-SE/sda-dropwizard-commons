package org.sdase.commons.server.morphia.codec;

import static java.time.ZoneOffset.UTC;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class ZonedDateTimeCodec implements Codec<ZonedDateTime> {

  @Override
  public ZonedDateTime decode(BsonReader reader, DecoderContext decoderContext) {
    var val = reader.readString();
    if (val == null) {
      return null;
    }

    // TODO Is that still possible?
//    if (val instanceof Date) {
//      return ZonedDateTime.ofInstant(((Date) val).toInstant(), UTC);
//    }

    try {
      return ZonedDateTime.ofInstant(ZonedDateTime.parse(val).toInstant(), UTC);
    }
    catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Can't convert to ZonedDateTime from " + val);
    }
  }

  @Override
  public void encode(BsonWriter writer, ZonedDateTime value, EncoderContext encoderContext) {
    if (value == null) {
      writer.writeNull();
    }
    else {
      writer.writeString(Date.from((value).toInstant()).toString());
    }
  }

  @Override
  public Class<ZonedDateTime> getEncoderClass() {
    return ZonedDateTime.class;
  }
}
