package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/** Writes `null` values of the given list is empty. Otherwise uses the default conversion. */
@WritingConverter
public class ListWritingConverter implements Converter<List<Object>, List<Object>> {

  private MongoConverter mongoConverter;

  @Override
  public List<Object> convert(List<Object> source) {
    if (source.isEmpty()) {
      return null;
    }

    return source.stream()
        .map(o -> mongoConverter.convertToMongoType(o))
        .collect(Collectors.toList());
  }

  public void setMongoConverter(MongoConverter mongoConverter) {
    this.mongoConverter = mongoConverter;
  }
}
