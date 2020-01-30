package org.sdase.commons.server.jackson.test;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;

@Resource
public class ResourceWithLink {

  @Link private HALLink self;

  public HALLink getSelf() {
    return self;
  }

  public ResourceWithLink setSelf(HALLink self) {
    this.self = self;
    return this;
  }
}
