package org.sdase.commons.server.spring.data.mongo.converter;

import java.time.ZonedDateTime;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Copied from <a
 * href="https://github.com/SDA-SE/sda-spring-boot-commons/blob/0.12.3/sda-commons-starter-mongodb/src/main/java/org/sdase/commons/spring/boot/mongodb/converter/ZonedDateTimeWriteConverter.java">SDA
 * Spring Boot Commons</a> to ensure compatibility.
 */
@WritingConverter
public enum ZonedDateTimeWriteConverter implements Converter<ZonedDateTime, Date> {
  INSTANCE;

  @Override
  public Date convert(ZonedDateTime source) {
    return Date.from(source.toInstant());
  }
}
