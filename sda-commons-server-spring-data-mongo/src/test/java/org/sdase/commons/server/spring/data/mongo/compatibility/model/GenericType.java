package org.sdase.commons.server.spring.data.mongo.compatibility.model;

public class GenericType<T> {

  private T value;

  public T getValue() {
    return value;
  }

  public GenericType<T> setValue(T value) {
    this.value = value;
    return this;
  }
}
