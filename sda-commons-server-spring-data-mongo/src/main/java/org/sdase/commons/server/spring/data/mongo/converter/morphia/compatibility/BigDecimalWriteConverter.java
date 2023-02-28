package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import java.math.BigDecimal;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public enum BigDecimalWriteConverter implements Converter<BigDecimal, Decimal128> {
  INSTANCE;

  @Override
  public Decimal128 convert(BigDecimal source) {
    return new Decimal128(source);
  }
}
