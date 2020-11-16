package org.sdase.commons.starter.example.people.db;

import java.util.List;

public class TestDataUtil {

  private TestDataUtil() {
    // utility class
  }

  public static void clearTestData() {
    PersonManager.peopleDatabase.clear();
  }

  public static PersonEntity addPersonEntity(String id, String firstName, String lastName) {
    return addPersonEntity(id, firstName, lastName, null);
  }

  public static PersonEntity addPersonEntity(
      String id, String firstName, String lastName, List<PersonEntity> parents) {
    PersonEntity entity = new PersonEntity(id, firstName, lastName);
    if (parents != null) {
      entity.getParents().addAll(parents);
      parents.forEach(parent -> parent.getChildren().add(entity));
    }
    PersonManager.peopleDatabase.put(id, entity);
    return entity;
  }
}
