package com.sdase.commons.server.weld.testing.test.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;
import java.io.Serializable;

public class FooProducer implements Serializable {

   private static final long serialVersionUID = 1L;

   @Produces
   @Named("foo")
   private final String foo = "foo";

}
