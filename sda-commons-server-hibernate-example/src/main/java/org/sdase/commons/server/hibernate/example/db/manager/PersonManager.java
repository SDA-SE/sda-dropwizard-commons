package org.sdase.commons.server.hibernate.example.db.manager;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.sdase.commons.server.hibernate.example.db.model.PersonEntity;

public class PersonManager extends AbstractDAO<PersonEntity> {

  /**
   * Creates a new DAO with a given session provider.
   *
   * @param sessionFactory a session provider
   */
  public PersonManager(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  @Override
  public PersonEntity persist(PersonEntity p) {
    return super.persist(p);
  }

  public PersonEntity getById(long id) {
    return super.get(id);
  }
}
