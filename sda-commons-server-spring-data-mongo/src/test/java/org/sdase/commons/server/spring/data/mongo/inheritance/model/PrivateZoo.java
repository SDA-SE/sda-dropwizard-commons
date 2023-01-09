package org.sdase.commons.server.spring.data.mongo.inheritance.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("zoo")
public class PrivateZoo {

  @MongoId private String id;

  private List<AbstractPet> pets = new ArrayList<>();

  public String getId() {
    return id;
  }

  public PrivateZoo setId(String id) {
    this.id = id;
    return this;
  }

  public List<AbstractPet> getPets() {
    return pets;
  }

  public PrivateZoo setPets(List<AbstractPet> pets) {
    this.pets = pets;
    return this;
  }
}
