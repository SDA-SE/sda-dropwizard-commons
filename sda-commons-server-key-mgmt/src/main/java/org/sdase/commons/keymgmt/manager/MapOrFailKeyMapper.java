package org.sdase.commons.keymgmt.manager;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.sdase.commons.keymgmt.model.KeyMappingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapOrFailKeyMapper implements KeyMapper {

  private static final Logger LOG = LoggerFactory.getLogger(MapOrFailKeyMapper.class);

  private static final String PLACEHOLDER = "UNKNOWN_KEY_VALUE";

  private final KeyMappingModel mappingModel;

  private final boolean useApiToImplPlaceholder;

  private final boolean useImplToApiPlaceholder;

  /**
   * Creates a new {@link MapOrFailKeyMapper}, optionally using placeholders for missing mappings.
   *
   * @param mappingModel the mapping model to use
   * @param useApiToImplPlaceholder if true, a placeholder is used for missing mappings from API to
   *     implementation
   * @param useImplToApiPlaceholder if true, a placeholder is used for missing mappings from
   *     implementation to API
   * @return a new {@link MapOrFailKeyMapper}
   */
  public MapOrFailKeyMapper(
      KeyMappingModel mappingModel,
      boolean useApiToImplPlaceholder,
      boolean useImplToApiPlaceholder) {
    this.mappingModel = mappingModel;
    this.useApiToImplPlaceholder = useApiToImplPlaceholder;
    this.useImplToApiPlaceholder = useImplToApiPlaceholder;
  }

  @Override
  public String toImpl(String value) {
    return mappingModel
        .getMapping()
        .mapToImpl(value)
        .or(
            () -> {
              if (useApiToImplPlaceholder) {
                LOG.warn(
                    "No mapping to implementation found for key '{}' and value '{}'. Substituting"
                        + " \"UNKNOWN_KEY_VALUE\"",
                    mappingModel.getName(),
                    value);
                return Optional.of(PLACEHOLDER);
              } else {
                return Optional.empty();
              }
            })
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
        .or(
            () -> {
              if (useImplToApiPlaceholder) {
                LOG.warn(
                    "No mapping to api found for key '{}' and value '{}'. Substituting"
                        + " \"UNKNOWN_KEY_VALUE\"",
                    mappingModel.getName(),
                    value);
                return Optional.of(PLACEHOLDER);
              } else {
                return Optional.empty();
              }
            })
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
