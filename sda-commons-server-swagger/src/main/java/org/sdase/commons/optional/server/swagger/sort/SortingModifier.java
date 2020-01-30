package org.sdase.commons.optional.server.swagger.sort;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Swagger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Sorts the Paths and Definitions alphabetically. */
@SwaggerDefinition
public class SortingModifier implements ReaderListener {

  @Override
  public void beforeScan(Reader reader, Swagger swagger) {
    // nothing to do here
  }

  @Override
  public void afterScan(Reader reader, Swagger swagger) {
    if (swagger == null) {
      return;
    }

    if (swagger.getPaths() != null) {
      swagger.setPaths(
          swagger.getPaths().entrySet().stream()
              .sorted(Entry.comparingByKey())
              .collect(
                  LinkedHashMap::new,
                  (map, item) -> map.put(item.getKey(), item.getValue()),
                  Map::putAll));
    }

    if (swagger.getDefinitions() != null) {
      swagger.setDefinitions(
          swagger.getDefinitions().entrySet().stream()
              .sorted(Entry.comparingByKey())
              .collect(
                  LinkedHashMap::new,
                  (map, item) -> map.put(item.getKey(), item.getValue()),
                  Map::putAll));
    }
  }
}
