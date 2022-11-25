package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public enum CharArrayReadConverter implements Converter<String, char[]> {
  INSTANCE;

  @Override
  public char[] convert(String value) {
    return value.toCharArray();
  }
}
