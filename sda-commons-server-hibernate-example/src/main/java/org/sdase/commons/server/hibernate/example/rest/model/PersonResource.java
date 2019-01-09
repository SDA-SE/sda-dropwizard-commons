package org.sdase.commons.server.hibernate.example.rest.model;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * REST resource data model
 */
public class PersonResource {

   private long id;

   @NotEmpty
   private String name;

   public long getId() {
      return id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }
}
