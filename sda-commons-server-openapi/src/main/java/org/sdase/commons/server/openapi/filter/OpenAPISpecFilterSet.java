package org.sdase.commons.server.openapi.filter;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.filter.OpenAPISpecFilter;
import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An implementation of the {@link OpenAPISpecFilter} that delegates the request to multiple
 * registered filters. Since swagger-core always creates a new instance of this filter, the
 * registered filters are stored statically.
 *
 * <p>These filters is applied on every call to /openapi.{yaml|json} and can inject information from
 * the request context.
 */
public class OpenAPISpecFilterSet extends AbstractSpecFilter {
  private static final List<OpenAPISpecFilter> filters = new ArrayList<>();
  private static final SpecFilter specFilter = new SpecFilter();

  /**
   * Register a filter that should be called when an OpenAPI file is returned to a caller.
   *
   * @see #clear()
   * @param filter the filter to register
   */
  public static void register(OpenAPISpecFilter filter) {
    filters.add(filter);
  }

  /** Clear all filters */
  public static void clear() {
    filters.clear();
  }

  @Override
  public Optional<OpenAPI> filterOpenAPI(
      OpenAPI openAPI,
      Map<String, List<String>> params,
      Map<String, String> cookies,
      Map<String, List<String>> headers) {

    OpenAPI result = openAPI;
    for (OpenAPISpecFilter filter : filters) {
      result = specFilter.filter(result, filter, params, cookies, headers);
      if (result == null) {
        return Optional.empty();
      }
    }
    return Optional.of(result);
  }
}
