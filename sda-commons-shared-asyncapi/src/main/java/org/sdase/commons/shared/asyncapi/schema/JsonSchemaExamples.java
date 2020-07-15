package org.sdase.commons.shared.asyncapi.schema;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

// TODO: This annotation should better be in an own package to import less dependencies in streaming
//  models

/**
 * Annotation to describe example values in a JSON schema.
 *
 * <pre>
 *   &#64;JsonSchemaExamples(value = {"Tesla Roadster", "Hummer H1"})
 *   private String name;
 * </pre>
 */
@Target({METHOD, FIELD, PARAMETER, TYPE})
@Retention(RUNTIME)
public @interface JsonSchemaExamples {

  /**
   * @return An array of example values, where each value can either be a string or a JSON value.
   */
  String[] value();
}
