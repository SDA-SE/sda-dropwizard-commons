package org.sdase.commons.server.morphia.test.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.validation.constraints.NotNull;
import org.bson.types.ObjectId;

@Entity("people") // required when entities are added by classpath scanning to ensure indexes
public class Person {

  @Id private ObjectId id;

  @Indexed @NotNull private String name;

  @Indexed private int age;

  private PhoneNumber phoneNumber;

  private LocalDate birthday;

  private LocalDateTime lastLogin;

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
}
