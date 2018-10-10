package com.sdase.commons.server.jackson.test;

import com.sdase.commons.server.jackson.EnableFieldFilter;
import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;

@Resource
@EnableFieldFilter
@SuppressWarnings("WeakerAccess")
public class PersonResource {

   @Link
   private HALLink self;

   private String firstName;

   private String lastName;

   private String nickName;

   public HALLink getSelf() {
      return self;
   }

   public PersonResource setSelf(HALLink self) {
      this.self = self;
      return this;
   }

   public String getFirstName() {
      return firstName;
   }

   public PersonResource setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
   }

   public String getLastName() {
      return lastName;
   }

   public PersonResource setLastName(String lastName) {
      this.lastName = lastName;
      return this;
   }

   public String getNickName() {
      return nickName;
   }

   public PersonResource setNickName(String nickName) {
      this.nickName = nickName;
      return this;
   }
}
