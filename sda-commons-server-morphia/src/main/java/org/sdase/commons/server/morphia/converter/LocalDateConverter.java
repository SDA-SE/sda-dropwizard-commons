package org.sdase.commons.server.morphia.converter;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import dev.morphia.converters.SimpleValueConverter;
import dev.morphia.converters.TypeConverter;
import dev.morphia.mapping.MappedField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class LocalDateConverter extends TypeConverter implements SimpleValueConverter {

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

  public LocalDateConverter() {
    super(LocalDate.class);
  }

  @Override
  public Object decode(
      Class<?> targetClass,
      Object fromDBObject,
      @SuppressWarnings("deprecation")
          MappedField optionalExtraInfo) { // NOSONAR deprecation inherited
    if (fromDBObject == null) {
      return null;
    }

    if (fromDBObject instanceof String) {
      return LocalDate.parse((String) fromDBObject, ISO_LOCAL_DATE_WITH_OPTIONAL_TIME);
    }

    // If this exception arises for java.util.Date, it is most likely that the collection has been
    // set up with an older version of SDA Commons which is not compatible with this converter.
    // The LocalDateConverter causing this breaking change has been introduced by PR #185. It should
    // help to initialize the MorphiaBundle 'withoutLocalDateConverter()' if upgrading the content
    // of the database is not an option.
    throw new IllegalArgumentException(
        String.format("Cannot decode object of class: %s", fromDBObject.getClass().getName()));
  }

  @Override
  public Object encode(
      Object value,
      @SuppressWarnings("deprecation")
          MappedField optionalExtraInfo) { // NOSONAR deprecation inherited
    if (value == null) {
      return null;
    }

    if (value instanceof LocalDate) {
      return ((LocalDate) value).format(ISO_LOCAL_DATE);
    }

    throw new IllegalArgumentException(
        String.format("Cannot encode object of class: %s", value.getClass().getName()));
  }
}
