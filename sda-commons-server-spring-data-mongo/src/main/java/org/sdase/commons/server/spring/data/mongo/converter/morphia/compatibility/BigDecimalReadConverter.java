package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import java.math.BigDecimal;
import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public enum BigDecimalReadConverter implements Converter<Decimal128, BigDecimal> {
  INSTANCE;

  @Override
  public BigDecimal convert(Decimal128 source) {
    return source.bigDecimalValue();
  }
}
