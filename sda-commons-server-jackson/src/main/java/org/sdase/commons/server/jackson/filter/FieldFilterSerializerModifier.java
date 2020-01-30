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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.sdase.commons.server.jackson.EnableFieldFilter;

/**
 * Applies the field filter when a bean is serialized. This class is called for each property, for
 * each serialized object.
 *
 * <p>It removes the fields by the following rules:
 *
 * <ol>
 *   <li>The property is returned if no field filter is set
 *   <li>The property is <b>not</b> returned if the field (at the top level) is <b>not</b> part of
 *       the set of filtered fields (&fields= parameter)
 *   <li>The property is returned if it is part of a nested or embedded object
 * </ol>
 */
public class FieldFilterSerializerModifier extends BeanSerializerModifier {

  private static final String FIELD_FILTER_QUERY_PARAM = "fields";

  @Context private UriInfo uriInfo;

  @Override
  public JsonSerializer<?> modifySerializer(
      SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
    if (serializer instanceof BeanSerializer
        && beanDesc.getBeanClass().isAnnotationPresent(EnableFieldFilter.class)) {
      return new FieldFilterSerializer((BeanSerializer) serializer, uriInfo);
    } else {
      return serializer;
    }
  }

  private static class FieldFilterSerializer extends BeanSerializer {
    FieldFilterSerializer(BeanSerializerBase src, UriInfo uriInfo) {
      super(src);
      for (int i = 0; i < _props.length; i++) {
        BeanPropertyWriter prop = _props[i];
        _props[i] = new SkipFieldBeanPropertyWriter(prop, uriInfo);
      }
    }

    private static class SkipFieldBeanPropertyWriter extends BeanPropertyWriter {

      private final transient UriInfo uriInfo;

      SkipFieldBeanPropertyWriter(BeanPropertyWriter prop, UriInfo uriInfo) {
        super(prop);
        this.uriInfo = uriInfo;
      }

      @Override
      public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
          throws Exception {
        if (!hasAnyFieldFilter() || isIncludedField() || isEmbeddedOrNested(gen)) {
          super.serializeAsField(bean, gen, prov);
        }
      }

      private boolean isEmbeddedOrNested(JsonGenerator generator) {
        Set<String> pathSet =
            getPath(generator.getOutputContext(), getName(), new LinkedHashSet<>());
        return isEmbedded(pathSet) || isNested(pathSet);
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

      private boolean isIncludedField() {
        Stream<String> requestedFields =
            uriInfo.getQueryParameters().get(FIELD_FILTER_QUERY_PARAM).stream()
                .map(fields -> fields.split(","))
                .flatMap(Stream::of)
                .map(String::trim);
        return requestedFields.anyMatch(fieldName -> fieldName.equals(getName()));
      }

      /**
       * Checks if the property is part of a nested object
       *
       * @param set the full path of the object
       * @return true if the property is part of a nested object
       */
      private boolean isNested(Set<String> set) {
        return set.size() > 1;
      }

      /**
       * Checks if the object is part to an embedded object
       *
       * @param set the full path of the object
       * @return true if the object is part to an embedded object
       */
      private boolean isEmbedded(Set<String> set) {
        return set.contains("_embedded");
      }

      /**
       * Get all nested path segments of the property. Works recursively.
       *
       * @param context the current context of the serializer
       * @param name a custom name that should be added as last element. If null, the default name
       *     is used. This might
       * @return the path segments from root -> [parent_node] -> ... -> your property
       */
      private Set<String> getPath(JsonStreamContext context, String name, Set<String> set) {
        JsonStreamContext parentContext = context.getParent();

        // Add all parent paths
        if (parentContext != null) {
          getPath(parentContext, null, set);
        }

        // add the provided name or read from the properties
        if (name != null) {
          set.add(name);
        } else if (context.getCurrentName() != null) {
          set.add(context.getCurrentName());
        }

        return set;
      }
    }
  }
}
