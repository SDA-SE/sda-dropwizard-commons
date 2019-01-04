package org.sdase.commons.server.weld.beans;

import javax.enterprise.inject.Produces;

public class ExampleProducer {

   @Produces
   public String someString() {
      return "some string";
   }

}
