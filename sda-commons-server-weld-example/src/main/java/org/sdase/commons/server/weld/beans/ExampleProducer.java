package org.sdase.commons.server.weld.beans;

import jakarta.enterprise.inject.Produces;

public class ExampleProducer {

  @Produces
  public String someString() {
    return "some string";
  }
}
