package org.sdase.commons.server.hibernate.testing.db.model;

import javax.persistence.*;

/** database entity model example */
@Entity
@Table(name = "person")
public class PersonEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column private String name;

  public long getId() {
    return id;
  }

  public PersonEntity setId(long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public PersonEntity setName(String name) {
    this.name = name;
    return this;
  }
}
