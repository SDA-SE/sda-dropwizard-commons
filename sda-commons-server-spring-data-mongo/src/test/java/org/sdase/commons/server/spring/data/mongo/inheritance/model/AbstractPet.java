package org.sdase.commons.server.spring.data.mongo.inheritance.model;

import org.springframework.data.annotation.TypeAlias;

public abstract class AbstractPet {

  private String name;

  public String getName() {
    return name;
  }

  public AbstractPet setName(String name) {
    this.name = name;
    return this;
  }

  @TypeAlias("dog")
  public static class Dog extends AbstractPet {}

  @TypeAlias("cat")
  public static class Cat extends AbstractPet {}
}
