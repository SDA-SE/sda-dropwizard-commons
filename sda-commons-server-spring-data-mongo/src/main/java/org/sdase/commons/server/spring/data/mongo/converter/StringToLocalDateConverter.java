package org.sdase.commons.server.spring.data.mongo.converter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToLocalDateConverter implements Converter<String, LocalDate> {

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
  public LocalDate convert(String value) {
    return LocalDate.parse(value, ISO_LOCAL_DATE_WITH_OPTIONAL_TIME);
  }
}
