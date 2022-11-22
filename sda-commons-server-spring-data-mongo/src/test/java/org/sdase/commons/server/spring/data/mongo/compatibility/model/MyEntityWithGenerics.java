package org.sdase.commons.server.spring.data.mongo.compatibility.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("MyEntityWithGenerics")
public class MyEntityWithGenerics {

  @MongoId private String id;

  private GenericType<?> genericValue;

  public String getId() {
    return id;
  }

  public MyEntityWithGenerics setId(String id) {
    this.id = id;
    return this;
  }

  public GenericType<?> getGenericValue() {
    return genericValue;
  }

  public MyEntityWithGenerics setGenericValue(GenericType<?> genericValue) {
    this.genericValue = genericValue;
    return this;
  }
}
