package org.sdase.commons.server.openapi.hal;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converter to handle HAL annotated classes.
 *
 * <p>It ensures embedded resources and links are arranged into an "_embedded" and "_links" object
 * respectively.
 */
@SuppressWarnings("java:S3740") // ignore "Raw types should not be used" introduced by swagger-core
public class HALModelResolver extends ModelResolver {
  /**
   * Holds all properties as follows:
   *
   * <pre>{ "Model Name" -> { "Property Name" -> "Type of Hal (links or embedded) } }</pre>
   */
  private final Map<String, Map<String, HalPropertyConfiguration>> componentHalProperties =
      new LinkedHashMap<>();

  /**
   * A cache of already resolved types. This is similar to what happens in {@link
   * io.swagger.v3.core.converter.ModelConverterContextImpl#resolve(AnnotatedType)}. If we don't
   * cache them by ourselves, we miss properties of same name and type (e.g. self).
   */
  private final HashMap<Type, Schema<?>> modelByType = new HashMap<>();

  public HALModelResolver(ObjectMapper mapper) {
    super(mapper);
  }

  /**
   * Gets called for each property of a model. It remembers which property should be part of the
   * _embedded or _links section for each model.
   */
  private void discoverHalProperties(AnnotatedType type) {
    if (type.getParent() != null && type.getCtxAnnotations() != null) {
      for (Annotation annotation : type.getCtxAnnotations()) {
        HALReservedProperty.valueOf(annotation.annotationType())
            .ifPresent(
                rp -> {
                  componentHalProperties
                      .computeIfAbsent(type.getParent().getName(), k -> new LinkedHashMap<>())
                      .put(
                          type.getPropertyName(),
                          new HalPropertyConfiguration(rp, rp.getValue(annotation)));

                  fixAnnotations(type);
                });
      }
    }
  }

  /**
   * Trick the upper class to cache individual types. The AnnotatedType is only checked for "name"
   * and "annotations" but doesn't take into account the parent class. Thus, this class might not
   * get called for a linked/embedded property in some cases.
   *
   * @param type the type that should be modified
   */
  private void fixAnnotations(AnnotatedType type) {
    // we only want to modify the annotations once
    if (Arrays.stream(type.getCtxAnnotations()).anyMatch(a -> a instanceof HalModifiedAnnotation)) {
      return;
    }

    Annotation[] annotations =
        Arrays.copyOf(type.getCtxAnnotations(), type.getCtxAnnotations().length + 1);

    annotations[annotations.length - 1] =
        new HalModifiedAnnotation() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return getClass();
          }
        };

    type.ctxAnnotations(annotations);
  }

  @Override
  public Schema resolve(
      AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
    Type t = type.getType();
    if (t instanceof JavaType) {
      t = ((JavaType) t).getRawClass();
    }

    // discover all HAL properties
    discoverHalProperties(type);

    // shortcut only for classes that we edited
    if (modelByType.containsKey(t)) {
      return modelByType.get(t);
    }

    // Let the original resolve logic create the Schema for the type
    Schema<?> model = super.resolve(type, context, chain);

    // move all HAL properties to the sub properties
    if (model != null
        && model.getProperties() != null
        && componentHalProperties.containsKey(model.getName())) {

      reorderHalProperties(model);

      modelByType.put(t, model);
    }

    return model;
  }

  /**
   * Move all properties that are marked as an HAL property to the respective sub-elements (i.e.
   * _links or _embedded).
   *
   * @param model the Schema to edit
   */
  private void reorderHalProperties(Schema<?> model) {
    Map<String, Schema<?>> properties = new LinkedHashMap<>();
    Set<String> originalProperties = new HashSet<>();

    Map<String, HalPropertyConfiguration> halProperties =
        componentHalProperties.get(model.getName());

    for (Map.Entry<String, Schema> entry : model.getProperties().entrySet()) {
      if (halProperties.containsKey(entry.getKey())) {
        HalPropertyConfiguration halPropertyConfiguration = halProperties.get(entry.getKey());
        String halType = halPropertyConfiguration.halType.getName();

        String name =
            halPropertyConfiguration.specificName.isEmpty()
                ? entry.getKey()
                : halPropertyConfiguration.specificName;

        properties
            .computeIfAbsent(halType, s -> new MapSchema())
            .addProperties(name, entry.getValue());
        originalProperties.add(entry.getKey());
      }
    }

    for (Map.Entry<String, Schema<?>> entry : properties.entrySet()) {
      model.addProperties(entry.getKey(), entry.getValue());
    }

    for (String propertyName : originalProperties) {
      model.getProperties().remove(propertyName);
    }
  }

  /**
   * A class that holds information about the type of hal and the name that should be used instead
   * of the original property name.
   */
  private static class HalPropertyConfiguration {
    public final HALReservedProperty halType;
    public final String specificName;

    public HalPropertyConfiguration(HALReservedProperty halType, String specificName) {
      this.halType = halType;
      this.specificName = specificName;
    }
  }

  /**
   * Enumeration of properties reserved for HAL along with the association to the annotation marking
   * objects to go into these properties.
   */
  public enum HALReservedProperty {
    LINKS("_links", Link.class),
    EMBEDDED("_embedded", EmbeddedResource.class);

    private final String name;
    private final Class<? extends Annotation> annotation;
    private final Method valueMethod;

    @SuppressWarnings("java:S112") // permit use of RuntimeException
    HALReservedProperty(String name, Class<? extends Annotation> annotation) {
      this.name = name;
      this.annotation = annotation;
      try {
        valueMethod = annotation.getDeclaredMethod("value");
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }

    public String getName() {
      return name;
    }

    @SuppressWarnings("java:S112") // permit use of RuntimeException
    public String getValue(Annotation annotation) {
      try {
        return (String) valueMethod.invoke(annotation);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new RuntimeException("Unable to get default value from annotation", e);
      }
    }

    public static Optional<HALReservedProperty> valueOf(Class<? extends Annotation> annotation) {
      for (HALReservedProperty rp : values()) {
        if (rp.annotation.equals(annotation)) {
          return Optional.of(rp);
        }
      }
      return Optional.empty();
    }
  }

  /** A custom annotation tto show that a type was already handled by the discovery logic. */
  public interface HalModifiedAnnotation extends Annotation {}
}
