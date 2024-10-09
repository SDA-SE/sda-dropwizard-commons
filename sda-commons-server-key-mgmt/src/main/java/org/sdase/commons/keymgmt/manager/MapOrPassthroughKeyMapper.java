package org.sdase.commons.keymgmt.manager;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.sdase.commons.keymgmt.model.KeyMappingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapOrPassthroughKeyMapper implements KeyMapper {

  private static final Logger LOG = LoggerFactory.getLogger(MapOrPassthroughKeyMapper.class);

  private final KeyMappingModel mappingModel;

  private final Optional<String> apiPlaceholder;

  private final Optional<String> implPlaceholder;

  public MapOrPassthroughKeyMapper(
      KeyMappingModel mappingModel, String apiPlaceholder, String implPlaceholder) {
    this.mappingModel = mappingModel;
    this.apiPlaceholder = Optional.ofNullable(apiPlaceholder);
    this.implPlaceholder = Optional.ofNullable(implPlaceholder);
  }

  @Override
  public String toImpl(String value) {
    return mappingModel
        .getMapping()
        .mapToImpl(value)
        .or(() -> implPlaceholder)
        .orElseGet(
            () -> {
              LOG.warn(
                  "No mapping to implementation found for key '{}' and value '{}'. Passing the"
                      + " value",
                  mappingModel.getName(),
                  value);
              return value;
            });
  }

  @Override
  public String toApi(String value) {
    return mappingModel
        .getMapping()
        .mapToApi(value)
        .or(() -> apiPlaceholder)
        .map(s -> s.toUpperCase(Locale.ROOT))
        .orElseGet(
            () -> {
              LOG.warn(
                  "No mapping to api found for key '{}' and value '{}'. Passing the value",
                  mappingModel.getName(),
                  value);
              return value;
            });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MapOrPassthroughKeyMapper that = (MapOrPassthroughKeyMapper) o;
    return mappingModel.equals(that.mappingModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mappingModel);
  }
}
