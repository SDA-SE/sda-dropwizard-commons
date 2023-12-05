/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi;

/**
 * Exception to be thrown when a class referenced as {@code $ref:
 * class://fully.qualified.classname.of.Model} in an AsyncAPI is not found in the classpath.
 */
public class ReferencedClassNotFoundException extends RuntimeException {

  public ReferencedClassNotFoundException(ClassNotFoundException e) {
    super(e);
  }
}
