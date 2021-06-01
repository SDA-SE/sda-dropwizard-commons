package org.sdase.commons.keymgmt.manager;

import java.util.Locale;
import java.util.Objects;
import org.sdase.commons.keymgmt.model.KeyMappingModel;

public class MapOrFailKeyMapper implements KeyMapper {

  private final KeyMappingModel mappingModel;

  public MapOrFailKeyMapper(KeyMappingModel mappingModel) {
    this.mappingModel = mappingModel;
  }

  @Override
  public String toImpl(String value) {
    return mappingModel
        .getMapping()
        .mapToImpl(value)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "No mapping to implementation for value '%s' found in key mapping for '%s'",
                        value, mappingModel.getName())));
  }

  @Override
  public String toApi(String value) {
    return mappingModel
        .getMapping()
        .mapToApi(value)
        .map(s -> s.toUpperCase(Locale.ROOT))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "No mapping for implementation value '%s' found in key mapping for '%s'",
                        value, mappingModel.getName())));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MapOrFailKeyMapper that = (MapOrFailKeyMapper) o;
    return mappingModel.equals(that.mappingModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mappingModel);
  }
}
