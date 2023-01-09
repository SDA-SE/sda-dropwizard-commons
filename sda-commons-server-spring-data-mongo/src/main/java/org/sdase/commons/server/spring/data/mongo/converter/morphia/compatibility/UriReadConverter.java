package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import java.net.URI;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public enum UriReadConverter implements Converter<String, URI> {
  INSTANCE;

  @Override
  public URI convert(String value) {
    // see
    // https://github.com/MorphiaOrg/morphia/blob/r1.6.0/morphia/src/main/java/dev/morphia/converters/URIConverter.java#L40
    return URI.create(value.replace("%46", "."));
  }
}
