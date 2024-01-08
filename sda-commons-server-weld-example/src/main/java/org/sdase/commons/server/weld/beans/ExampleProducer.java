package org.sdase.commons.server.weld.beans;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

public class ExampleProducer {

  @Produces
  @Named("some-string")
  public String someString() {
    return "some string";
  }
}
