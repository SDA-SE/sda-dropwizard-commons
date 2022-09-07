package org.sdase.commons.server.spring.data.mongo.converter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

import java.time.LocalDate;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class LocalDateToStringConverter implements Converter<LocalDate, String> {

  @Override
  public String convert(LocalDate value) {
    return value.format(ISO_LOCAL_DATE);
  }
}
