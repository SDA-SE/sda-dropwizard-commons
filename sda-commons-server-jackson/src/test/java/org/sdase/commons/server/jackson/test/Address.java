package org.sdase.commons.server.jackson.test;

import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import org.sdase.commons.server.jackson.EnableFieldFilter;

@Resource
@EnableFieldFilter
public class Address {

   private String id;

   private String city;

   public String getCity() {
      return city;
   }

   public Address setCity(String city) {
      this.city = city;
      return this;
   }

   public String getId() {
      return id;
   }

   public Address setId(String id) {
      this.id = id;
      return this;
   }
}
