package org.sdase.commons.server.spring.data.mongo.example.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("people")
public class Person {

  @MongoId private ObjectId id;

  @Indexed @NotNull private String name;

  @Min(1)
  @Indexed
  private int age;

  private PhoneNumber phoneNumber;

  private LocalDate birthday;

  private LocalDateTime lastLogin;

  private ZonedDateTime zonedDateTime;

  @Field("_class")
  private String entityClass;

  @SuppressWarnings("unused")
  public ObjectId getId() {
    return id;
  }

  @SuppressWarnings("unused")
  public Person setId(ObjectId id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Person setName(String name) {
    this.name = name;
    return this;
  }

  public int getAge() {
    return age;
  }

  public Person setAge(int age) {
    this.age = age;
    return this;
  }

  public PhoneNumber getPhoneNumber() {
    return phoneNumber;
  }

  public Person setPhoneNumber(PhoneNumber phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public LocalDate getBirthday() {
    return birthday;
  }

  public Person setBirthday(LocalDate birthday) {
    this.birthday = birthday;
    return this;
  }

  public LocalDateTime getLastLogin() {
    return lastLogin;
  }

  public Person setLastLogin(LocalDateTime lastLogin) {
    this.lastLogin = lastLogin;
    return this;
  }

  public ZonedDateTime getZonedDateTime() {
    return zonedDateTime;
  }

  public Person setZonedDateTime(ZonedDateTime zonedDateTime) {
    this.zonedDateTime = zonedDateTime;
    return this;
  }

  public String getEntityClass() {
    return entityClass;
  }

  @SuppressWarnings("unused")
  public void setEntityClass(String entityClass) {
    this.entityClass = entityClass;
  }
}
