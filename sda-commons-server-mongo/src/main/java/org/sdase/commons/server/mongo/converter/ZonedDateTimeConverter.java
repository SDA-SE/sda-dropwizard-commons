package org.sdase.commons.server.mongo.converter;

import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;

import java.time.ZonedDateTime;
import java.util.Date;

import static java.time.ZoneOffset.UTC;

public class ZonedDateTimeConverter extends TypeConverter implements SimpleValueConverter {


   /**
    * Creates the Converter.
    */
   public ZonedDateTimeConverter() {
      this(ZonedDateTime.class);
   }

   protected ZonedDateTimeConverter(final Class<ZonedDateTime> clazz) {
      super(clazz);
   }

   @Override
   public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
      if (val == null) {
         return null;
      }
      if (val instanceof Date) {
         return ZonedDateTime.ofInstant(((Date) val).toInstant(), UTC);
      }

      if (val instanceof String) {
         return ZonedDateTime.ofInstant(ZonedDateTime.parse((String) val).toInstant(), UTC);
      }

      throw new IllegalArgumentException("Can't convert to ZonedDateTime from " + val);
   }

   @Override
   public Object encode(final Object value, final MappedField optionalExtraInfo) {
      if (value == null) {
         return null;
      }
      if (value instanceof ZonedDateTime) {
         return Date.from(((ZonedDateTime) value).toInstant());
      }

      throw new IllegalArgumentException("Can't convert to ZonedDateTime from " + value);
   }
}
