package org.sdase.commons.server.spring.data.mongo.example.model;

import org.sdase.commons.server.spring.data.mongo.annotation.DocumentType;

@DocumentType("dog")
public class Dog extends Animal {

  private String breed;

  @SuppressWarnings("unused")
  public String getBreed() {
    return breed;
  }

  public void setBreed(String breed) {
    this.breed = breed;
  }
}
