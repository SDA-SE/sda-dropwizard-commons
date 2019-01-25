package org.sdase.commons.server.morphia.test.model;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

@Entity("people") // required when entities are added by classpath scanning to ensure indexes
public class Person {

   @Id
   private ObjectId id;

   @Indexed
   private String name;

   @Indexed
   private int age;

   private PhoneNumber phoneNumber;

   @SuppressWarnings("unused")
   public ObjectId getId() {
      return id;
   }

   @SuppressWarnings("unused")
   public Person setId(ObjectId id) {
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

   public int getAge() {
      return age;
   }

   public Person setAge(int age) {
      this.age = age;
      return this;
   }

   public PhoneNumber getPhoneNumber() {
      return phoneNumber;
   }

   public Person setPhoneNumber(PhoneNumber phoneNumber) {
      this.phoneNumber = phoneNumber;
      return this;
   }
}
