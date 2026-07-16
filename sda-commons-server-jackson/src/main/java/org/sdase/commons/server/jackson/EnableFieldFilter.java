package org.sdase.commons.server.jackson;

import io.openapitools.jackson.dataformat.hal.JacksonHALModule;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sdase.commons.server.jackson.filter.JacksonFieldFilterModule;

/**
 * Enables field filtering for the annotated resource when rendered in a response. Clients may
 * request a lighter response containing only requested fields and HAL links or embedded resources
 * instead of the complete model.
 *
 * <p>If an annotated resource has three properties:
 *
 * <pre>
 *   &#64;EnableFieldFilter
 *   public class Person {
 *     private String firstName;
 *     private String surName;
 *     private String nickName;
 *     // ...
 *   }
 * </pre>
 *
 * <p>And the client requests {@code GET /person/ID?fields=firstName,surName} or {@code GET
 * /person/ID?fields=firstName&fields=surName}, the returned Json will be:
 *
 * <pre>
 *   {
 *     "firstName": "John",
 *     "surName": "Doe"
 *   }
 * </pre>
 *
 * <p>By default, nested annotated properties keep their full subtree. Set {@link
 * #enableNestedPathFiltering()} to {@code true} to enable filtering for nested field paths inside that
 * type.
 *
 * <p>Nested path filtering is configured per annotated type. If a nested annotated child should
 * also filter its own nested paths, that child type must opt in separately.
 *
 * <p>The {@link JacksonFieldFilterModule} has to be registered after the {@link JacksonHALModule}
 * is added to the {@link com.fasterxml.jackson.databind.ObjectMapper}. Both can be accomplished in
 * appropriate order, using the {@link JacksonConfigurationBundle}.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableFieldFilter {

  /**
   * Enables filtering by nested field paths for the annotated type.
   *
   * <p>Default: {@code false}. In default mode, once a nested property is included, its nested
   * subtree is serialized. If set to {@code true}, nested properties in this type are matched
   * against field paths and excluded nested fields are omitted.
   */
  boolean enableNestedPathFiltering() default false;
}
