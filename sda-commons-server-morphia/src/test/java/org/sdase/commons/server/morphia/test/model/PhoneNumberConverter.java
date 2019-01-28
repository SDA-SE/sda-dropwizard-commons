package org.sdase.commons.server.morphia.test.model;

import org.apache.commons.lang3.StringUtils;
import xyz.morphia.converters.SimpleValueConverter;
import xyz.morphia.converters.TypeConverter;
import xyz.morphia.mapping.MappedField;

public class PhoneNumberConverter extends TypeConverter implements SimpleValueConverter {
   public PhoneNumberConverter() {
      super(PhoneNumber.class);
   }

   @Override
   public Object decode(final Class<?> targetClass, final Object val, final MappedField optionalExtraInfo) {
      if (val == null) {
         return null;
      }

      if (val instanceof String) {
         String[] split = val.toString().split(" ");
         return new PhoneNumber().setCountryCode(split[0]).setAreaCode(split[1]).setNumber(split[2]);
      }

      throw new IllegalArgumentException("Can't convert to PhoneNumber from " + val);
   }

   @Override
   public Object encode(final Object value, final MappedField optionalExtraInfo) {
      if (value == null) {
         return null;
      }
      if (value instanceof PhoneNumber) {
         PhoneNumber phoneNumber = (PhoneNumber) value;
         String countryCode = StringUtils.trimToEmpty(phoneNumber.getCountryCode())
               .replaceAll("[^0-9+]", "")
               .replaceAll("^00", "+");
         String areaCode = StringUtils.trimToEmpty(phoneNumber.getAreaCode())
               .replaceAll("[^0-9]", "")
               .replaceAll("^0", "");
         String number = StringUtils.trimToEmpty(phoneNumber.getNumber())
               .replaceAll("[^0-9]", "");
         return countryCode + " " + areaCode + " " + number;
      }

      throw new IllegalArgumentException("Can't convert as PhoneNumber from " + value);
   }
}
