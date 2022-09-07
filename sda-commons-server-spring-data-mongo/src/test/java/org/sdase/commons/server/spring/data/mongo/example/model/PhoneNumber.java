package org.sdase.commons.server.spring.data.mongo.example.model;

public class PhoneNumber {

  /** e.g. 0049 or +49 */
  private String countryCode;

  /** e.g. 0172 */
  private String areaCode;

  /** e.g. 12345678 */
  private String number;

  public String getCountryCode() {
    return countryCode;
  }

  public PhoneNumber setCountryCode(String countryCode) {
    this.countryCode = countryCode;
    return this;
  }

  public String getAreaCode() {
    return areaCode;
  }

  public PhoneNumber setAreaCode(String areaCode) {
    this.areaCode = areaCode;
    return this;
  }

  public String getNumber() {
    return number;
  }

  public PhoneNumber setNumber(String number) {
    this.number = number;
    return this;
  }
}
