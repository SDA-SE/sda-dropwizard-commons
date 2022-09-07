package org.sdase.commons.server.spring.data.mongo.converter;

import static java.time.ZoneOffset.UTC;

import java.time.ZonedDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {

  @Override
  public ZonedDateTime convert(String source) {
    return ZonedDateTime.ofInstant(ZonedDateTime.parse(source).toInstant(), UTC);
  }
}
