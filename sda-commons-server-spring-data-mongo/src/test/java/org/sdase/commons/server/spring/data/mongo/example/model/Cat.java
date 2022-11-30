package org.sdase.commons.server.spring.data.mongo.example.model;

import org.sdase.commons.server.spring.data.mongo.annotation.DocumentType;

@DocumentType("cat")
public class Cat extends Animal {

  private int age;

  @SuppressWarnings("unused")
  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }
}
