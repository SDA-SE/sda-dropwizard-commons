package org.sdase.commons.server.spring.data.mongo.converter;

import static java.time.ZoneOffset.UTC;

import java.time.ZonedDateTime;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class DateToZonedDateTimeConverter implements Converter<Date, ZonedDateTime> {

  @Override
  public ZonedDateTime convert(Date source) {
    return ZonedDateTime.ofInstant(source.toInstant(), UTC);
  }
}
