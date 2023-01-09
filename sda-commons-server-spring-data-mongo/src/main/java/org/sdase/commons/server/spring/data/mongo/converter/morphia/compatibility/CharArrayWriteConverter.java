package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public enum CharArrayWriteConverter implements Converter<char[], String> {
  INSTANCE;

  @Override
  public String convert(char[] value) {
    return new String(value);
  }
}
