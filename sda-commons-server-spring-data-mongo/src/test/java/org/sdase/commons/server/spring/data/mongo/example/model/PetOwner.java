package org.sdase.commons.server.spring.data.mongo.example.model;

import javax.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("pet_owner")
public class PetOwner {

  @MongoId private ObjectId id;

  @Indexed @NotNull private String name;

  private Animal pet;

  @Field("_class")
  private String entityClass;

  @SuppressWarnings("unused")
  public ObjectId getId() {
    return id;
  }

  @SuppressWarnings("unused")
  public PetOwner setId(ObjectId id) {
    this.id = id;
    return this;
  }

  @SuppressWarnings("unused")
  public String getName() {
    return name;
  }

  @SuppressWarnings("unused")
  public void setName(String name) {
    this.name = name;
  }

  public Animal getPet() {
    return pet;
  }

  public void setPet(Animal pet) {
    this.pet = pet;
  }

  public String getEntityClass() {
    return entityClass;
  }

  @SuppressWarnings("unused")
  public void setEntityClass(String entityClass) {
    this.entityClass = entityClass;
  }
}
