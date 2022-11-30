package org.sdase.commons.server.spring.data.mongo.example.model;

import javax.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

public abstract class Animal {

  @MongoId private ObjectId id;

  @Indexed @NotNull private String name;

  private Double weight;

  private String color;

  @Field("_class")
  private String entityClass;

  @SuppressWarnings("unused")
  public ObjectId getId() {
    return id;
  }

  @SuppressWarnings("unused")
  public Animal setId(ObjectId id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @SuppressWarnings("unused")
  public Double getWeight() {
    return weight;
  }

  public void setWeight(Double weight) {
    this.weight = weight;
  }

  @SuppressWarnings("unused")
  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getEntityClass() {
    return entityClass;
  }

  @SuppressWarnings("unused")
  public void setEntityClass(String entityClass) {
    this.entityClass = entityClass;
  }
}
