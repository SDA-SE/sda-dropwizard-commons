package org.sdase.commons.server.spring.data.mongo.compatibility.model;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("MyEntity")
public class MyEntity {

  @MongoId private String id;

  private String stringValue;

  private char charValue;

  private MyEnum enumValue;

  private char[] charArrayValue;

  private URI uriValue;

  private Locale localeValue;

  private Currency currencyValue;

  private BigDecimal bigDecimalValue;

  private Date dateValue;

  private Instant instantValue;

  private LocalDateTime localDateTimeValue;

  public String getId() {
    return id;
  }

  public MyEntity setId(String id) {
    this.id = id;
    return this;
  }

  public String getStringValue() {
    return stringValue;
  }

  public MyEntity setStringValue(String stringValue) {
    this.stringValue = stringValue;
    return this;
  }

  public char getCharValue() {
    return charValue;
  }

  public MyEntity setCharValue(char charValue) {
    this.charValue = charValue;
    return this;
  }

  public MyEnum getEnumValue() {
    return enumValue;
  }

  public MyEntity setEnumValue(MyEnum enumValue) {
    this.enumValue = enumValue;
    return this;
  }

  public char[] getCharArrayValue() {
    return charArrayValue;
  }

  public MyEntity setCharArrayValue(char[] charArrayValue) {
    this.charArrayValue = charArrayValue;
    return this;
  }

  public URI getUriValue() {
    return uriValue;
  }

  public MyEntity setUriValue(URI uriValue) {
    this.uriValue = uriValue;
    return this;
  }

  public Locale getLocaleValue() {
    return localeValue;
  }

  public MyEntity setLocaleValue(Locale localeValue) {
    this.localeValue = localeValue;
    return this;
  }

  public Currency getCurrencyValue() {
    return currencyValue;
  }

  public MyEntity setCurrencyValue(Currency currencyValue) {
    this.currencyValue = currencyValue;
    return this;
  }

  public BigDecimal getBigDecimalValue() {
    return bigDecimalValue;
  }

  public MyEntity setBigDecimalValue(BigDecimal bigDecimalValue) {
    this.bigDecimalValue = bigDecimalValue;
    return this;
  }

  public Date getDateValue() {
    return dateValue;
  }

  public MyEntity setDateValue(Date dateValue) {
    this.dateValue = dateValue;
    return this;
  }

  public Instant getInstantValue() {
    return instantValue;
  }

  public MyEntity setInstantValue(Instant instantValue) {
    this.instantValue = instantValue;
    return this;
  }

  public LocalDateTime getLocalDateTimeValue() {
    return localDateTimeValue;
  }

  public MyEntity setLocalDateTimeValue(LocalDateTime localDateTimeValue) {
    this.localDateTimeValue = localDateTimeValue;
    return this;
  }
}
