package org.sdase.commons.server.weld.testing.test.util;

import java.io.Serializable;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

public class FooProducer implements Serializable {

  private static final long serialVersionUID = 1L;

  @Produces
  @Named("foo")
  private final String foo = "foo";
}
