package org.sdase.commons.server.spring.data.mongo.compatibility.model;

public class MyColor {

  public int red;
  public int green;
  public int blue;

  public int getRed() {
    return red;
  }

  public MyColor setRed(int red) {
    this.red = red;
    return this;
  }

  public int getGreen() {
    return green;
  }

  public MyColor setGreen(int green) {
    this.green = green;
    return this;
  }

  public int getBlue() {
    return blue;
  }

  public MyColor setBlue(int blue) {
    this.blue = blue;
    return this;
  }
}
