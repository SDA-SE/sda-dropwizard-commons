package org.sdase.commons.server.spring.data.mongo.example.model;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class StringToPhoneNumberConverter implements Converter<String, PhoneNumber> {
  @Override
  public PhoneNumber convert(String val) {
    String[] split = val.split(" ");
    return new PhoneNumber().setCountryCode(split[0]).setAreaCode(split[1]).setNumber(split[2]);
  }
}
