package org.sdase.commons.keymgmt.manager;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.sdase.commons.keymgmt.model.KeyMappingModel;

public class MapOrFailKeyMapper implements KeyMapper {

  private final KeyMappingModel mappingModel;

  private final boolean useApiPlaceholder;

  private final boolean useImplPlaceholder;

  public MapOrFailKeyMapper(
      KeyMappingModel mappingModel, boolean useApiPlaceholder, boolean useImplPlaceholder) {
    this.mappingModel = mappingModel;
    this.useApiPlaceholder = useApiPlaceholder;
    this.useImplPlaceholder = useImplPlaceholder;
  }

  @Override
  public String toImpl(String value) {
    return mappingModel
        .getMapping()
        .mapToImpl(value)
        .or(() -> useImplPlaceholder ? Optional.of("PLACEHOLDER") : Optional.empty())
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
        .or(() -> useApiPlaceholder ? Optional.of("PLACEHOLDER") : Optional.empty())
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
