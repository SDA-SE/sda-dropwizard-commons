package org.sdase.commons.server.jackson.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.sdase.commons.server.jackson.EnableFieldFilter;

/**
 * Applies the field filter when a bean is serialized. This class is called for each property, for
 * each serialized object.
 *
 * <p>It removes the fields by the following rules:
 *
 * <ol>
 *   <li>The property is returned if no field filter is set
 *   <li>The property is <b>not</b> returned if the field path is <b>not</b> part of the filtered
 *       fields ({@code &fields=} parameter)
 *   <li>The property is returned if it is part of an embedded object
 *   <li>If {@link EnableFieldFilter#filterNestedPaths()} is {@code false}, nested properties keep
 *       the full subtree once the parent property is included
 * </ol>
 *
 * <p>Nested field paths are only applied inside nested object types that are also annotated. If a
 * nested object type is not annotated, selecting one of its sub-fields keeps the whole nested
 * object. If a nested annotated type should also filter by nested paths, it must set {@link
 * EnableFieldFilter#filterNestedPaths()} to {@code true}.
 */
public class FieldFilterSerializerModifier extends BeanSerializerModifier {

  private static final String FIELD_FILTER_QUERY_PARAM = "fields";

  @Context private UriInfo uriInfo;

  @Override
  public JsonSerializer<?> modifySerializer(
      SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
    EnableFieldFilter fieldFilterAnnotation = getFieldFilterAnnotation(beanDesc);
    if (serializer instanceof BeanSerializer beanSerializer && fieldFilterAnnotation != null) {
      return new FieldFilterSerializer(
          beanSerializer, uriInfo, fieldFilterAnnotation.filterNestedPaths());
    } else {
      return serializer;
    }
  }

  private static EnableFieldFilter getFieldFilterAnnotation(BeanDescription beanDesc) {
    return beanDesc.getBeanClass().getAnnotation(EnableFieldFilter.class);
  }

  private static class FieldFilterSerializer extends BeanSerializer {
    FieldFilterSerializer(BeanSerializerBase src, UriInfo uriInfo, boolean filterNestedPaths) {
      super(src);
      for (int i = 0; i < _props.length; i++) {
        BeanPropertyWriter prop = _props[i];
        _props[i] = new SkipFieldBeanPropertyWriter(prop, uriInfo, filterNestedPaths);
      }
    }

    private static class SkipFieldBeanPropertyWriter extends BeanPropertyWriter {

      private final transient UriInfo uriInfo;
      private final boolean filterNestedPaths;

      SkipFieldBeanPropertyWriter(
          BeanPropertyWriter prop, UriInfo uriInfo, boolean filterNestedPaths) {
        super(prop);
        this.uriInfo = uriInfo;
        this.filterNestedPaths = filterNestedPaths;
      }

      @Override
      public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
          throws Exception {
        List<String> currentFieldPath =
            getPath(gen.getOutputContext(), getName(), new ArrayList<>());
        if (!hasAnyFieldFilter()
            || shouldKeepFullNestedSubtree(currentFieldPath)
            || isIncludedField(currentFieldPath)
            || isEmbedded(currentFieldPath)) {
          super.serializeAsField(bean, gen, prov);
        }
      }

      private boolean shouldKeepFullNestedSubtree(List<String> currentFieldPath) {
        return !filterNestedPaths && currentFieldPath.size() > 1;
      }

      private boolean hasAnyFieldFilter() {
        try {
          List<String> fieldFilters = uriInfo.getQueryParameters().get(FIELD_FILTER_QUERY_PARAM);
          return fieldFilters != null && !fieldFilters.isEmpty();
        } catch (Exception ignored) {
          // maybe there is some odd state, e.g. not in a request context
          return false;
        }
      }

      private boolean isIncludedField(List<String> currentPath) {
        Stream<String> requestedFields =
            uriInfo.getQueryParameters().get(FIELD_FILTER_QUERY_PARAM).stream()
                .map(fields -> fields.split(","))
                .flatMap(Stream::of)
                .map(String::trim)
                .filter(fieldName -> !fieldName.isEmpty());
        String serializedPath = String.join(".", currentPath);
        return requestedFields.anyMatch(requestedPath -> matches(requestedPath, serializedPath));
      }

      /**
       * Checks if the object is part to an embedded object
       *
       * @param path the full path of the object
       * @return true if the object is part to an embedded object
       */
      private boolean isEmbedded(List<String> path) {
        return path.contains("_embedded");
      }

      /**
       * Checks whether the current path should be included for a requested path.
       *
       * @param requestedPath the field path that should be included
       * @param currentPath the serialized field path currently being evaluated
       * @return true if the paths match exactly, if the current path is a parent of the requested
       *     path, or if the current path is a child of the requested path
       */
      private boolean matches(String requestedPath, String currentPath) {
        return currentPath.equals(requestedPath)
            || requestedPath.startsWith(currentPath + ".")
            || currentPath.startsWith(requestedPath + ".");
      }

      /**
       * Get all nested path segments of the property. Works recursively.
       *
       * @param context the current context of the serializer
       * @param name a custom name that should be added as last element. If null, the default name
       *     is used. This might
       * @return the path segments from root -> [parent_node] -> ... -> your property
       */
      private List<String> getPath(JsonStreamContext context, String name, List<String> path) {
        JsonStreamContext parentContext = context.getParent();

        // Add all parent paths
        if (parentContext != null) {
          getPath(parentContext, null, path);
        }

        // add the provided name or read from the properties
        if (name != null) {
          path.add(name);
        } else if (context.getCurrentName() != null) {
          path.add(context.getCurrentName());
        }

        return path;
      }
    }
  }
}
