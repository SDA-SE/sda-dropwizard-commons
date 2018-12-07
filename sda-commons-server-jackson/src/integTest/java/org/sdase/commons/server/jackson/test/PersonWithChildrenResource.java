package org.sdase.commons.server.jackson.test;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import org.sdase.commons.server.jackson.EnableFieldFilter;

import java.util.List;

@Resource
@EnableFieldFilter
@SuppressWarnings("WeakerAccess")
public class PersonWithChildrenResource {

   @Link
   private HALLink self;

   private String firstName;

   private String lastName;

   private String nickName;

   private List<PersonResource> children;

   public HALLink getSelf() {
      return self;
   }

   public PersonWithChildrenResource setSelf(HALLink self) {
      this.self = self;
      return this;
   }

   public String getFirstName() {
      return firstName;
   }

   public PersonWithChildrenResource setFirstName(String firstName) {
      this.firstName = firstName;
      return this;
   }

   public String getLastName() {
      return lastName;
   }

   public PersonWithChildrenResource setLastName(String lastName) {
      this.lastName = lastName;
      return this;
   }

   public String getNickName() {
      return nickName;
   }

   public PersonWithChildrenResource setNickName(String nickName) {
      this.nickName = nickName;
      return this;
   }

   public List<PersonResource> getChildren() {
      return children;
   }

   public PersonWithChildrenResource setChildren(List<PersonResource> children) {
      this.children = children;
      return this;
   }
}
