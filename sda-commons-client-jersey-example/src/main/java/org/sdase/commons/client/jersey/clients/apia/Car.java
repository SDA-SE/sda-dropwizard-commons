package org.sdase.commons.client.jersey.clients.apia;

public class Car {
  private String sign;
  private String color;

  public String getSign() {
    return sign;
  }

  public Car setSign(String sign) {
    this.sign = sign;
    return this;
  }

  public String getColor() {
    return color;
  }

  public Car setColor(String color) {
    this.color = color;
    return this;
  }
}
