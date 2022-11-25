/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.server.spring.data.mongo.converter;

import static java.time.ZoneOffset.UTC;

import java.time.ZonedDateTime;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Copied from <a
 * href="https://github.com/SDA-SE/sda-spring-boot-commons/blob/0.12.3/sda-commons-starter-mongodb/src/main/java/org/sdase/commons/spring/boot/mongodb/converter/ZonedDateTimeReadConverter.java">SDA
 * Spring Boot Commons</a> to ensure compatibility.
 */
@ReadingConverter
public enum ZonedDateTimeReadConverter implements Converter<Object, ZonedDateTime> {
  INSTANCE;

  @Override
  public ZonedDateTime convert(Object fromDBObject) {
    if (fromDBObject instanceof Date) {
      return ZonedDateTime.ofInstant(((Date) fromDBObject).toInstant(), UTC);
    }

    if (fromDBObject instanceof String) {
      return ZonedDateTime.ofInstant(ZonedDateTime.parse(fromDBObject.toString()).toInstant(), UTC);
    }

    throw new IllegalArgumentException("Can't convert to ZonedDateTime from " + fromDBObject);
  }
}
