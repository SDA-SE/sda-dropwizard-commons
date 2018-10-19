package com.sdase.commons.server.hibernate.test.model;

import javax.persistence.*;

@Entity
@Table(name = "person")
public class Person {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long id;

   @Column
   private String name;

   @Column
   private String email;

   public long getId() {
      return id;
   }

   public Person setId(long id) {
      this.id = id;
      return this;
   }

   public String getName() {
      return name;
   }

   public Person setName(String name) {
      this.name = name;
      return this;
   }

   public String getEmail() {
      return email;
   }

   public Person setEmail(String email) {
      this.email = email;
      return this;
   }
}
