package org.sdase.commons.server.weld.testing.test.util;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import java.io.Serializable;

public class FooProducer implements Serializable {

  @Produces
  @Named("foo")
  private final String foo = "foo";
}
