package org.sdase.commons.starter.example.people.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Mock implementation of a database manager for {@link PersonEntity}. */
public class PersonManager {

  /**
   * The mock of the underlying people database contains the family Jane, John and Jasmine Doe
   * initially. It is not private in this example to initialize test data. Usually a data base rule
   * for the used db will be used to populate test data.
   */
  static final Map<String, PersonEntity> peopleDatabase = new LinkedHashMap<>();

  public List<PersonEntity> findAll() {
    return new ArrayList<>(peopleDatabase.values());
  }

  public PersonEntity findById(String id) {
    return peopleDatabase.get(id);
  }
}
