package org.sdase.commons.server.spring.data.mongo.compatibility.model;

import java.util.List;

public class MySubEntity {

  private List<MyColor> colors;

  public List<MyColor> getColors() {
    return colors;
  }

  public MySubEntity setColors(List<MyColor> colors) {
    this.colors = colors;
    return this;
  }
}
