package org.sdase.commons.server.weld.testing.test.util;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.function.Supplier;

public class BarSupplier implements Supplier<String> {

  @Inject
  @Named("foo")
  private String foo;

  @Override
  public String get() {
    return foo;
  }
}
