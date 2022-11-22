package org.sdase.commons.server.spring.data.mongo.compatibility.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("MyEntity")
public class MyEntity {

  @MongoId private String id;

  private String value;

  public String getId() {
    return id;
  }

  public MyEntity setId(String id) {
    this.id = id;
    return this;
  }

  public String getValue() {
    return value;
  }

  public MyEntity setValue(String value) {
    this.value = value;
    return this;
  }
}
