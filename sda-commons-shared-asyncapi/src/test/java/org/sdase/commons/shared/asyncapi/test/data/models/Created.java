/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.test.data.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@SuppressWarnings("unused")
public class Created {

  @NotNull
  @Pattern(regexp = "[a-zA-Z0-9-_]{10,}")
  private String id;

  @NotBlank private String name;

  public String getId() {
    return id;
  }

  public Created setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Created setName(String name) {
    this.name = name;
    return this;
  }
}
