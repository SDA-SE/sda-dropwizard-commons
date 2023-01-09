package org.sdase.commons.server.spring.data.mongo.example.model;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class PhoneNumberToStringConverter implements Converter<PhoneNumber, String> {

  @Override
  public String convert(PhoneNumber phoneNumber) {
    String countryCode =
        StringUtils.trimToEmpty(phoneNumber.getCountryCode())
            .replaceAll("[^0-9+]", "")
            .replaceAll("^00", "+");
    String areaCode =
        StringUtils.trimToEmpty(phoneNumber.getAreaCode())
            .replaceAll("[^0-9]", "")
            .replaceAll("^0", "");
    String number = StringUtils.trimToEmpty(phoneNumber.getNumber()).replaceAll("[^0-9]", "");
    return countryCode + " " + areaCode + " " + number;
  }
}
