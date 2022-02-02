/*
 * Copyright (c). SDA SE Open Industry Solutions (https://www.sda.se).
 *
 * All rights reserved.
 */
package org.sdase.commons.server.hibernate.testing;

import io.dropwizard.testing.common.DAOTest;
import java.util.concurrent.Callable;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/*
 * This class is very similar to io.dropwizard.testing.junit5.DAOTestExtension,
 * but is based on org.junit.jupiter.api.extension.Extension which allows using @RegisterExtension
 */
public class DAOClassExtension implements BeforeAllCallback, AfterAllCallback {
  private final DAOTest daoTest;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    try {
      daoTest.before();
    } catch (Throwable e) {
      throw new HibernateException(e.getMessage());
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    daoTest.after();
  }

  public static class Builder extends DAOTest.Builder<DAOClassExtension.Builder> {
    public DAOClassExtension build() {
      return new DAOClassExtension(buildDAOTest());
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  private DAOClassExtension(DAOTest daoTest) {
    this.daoTest = daoTest;
  }

  public SessionFactory getSessionFactory() {
    return daoTest.getSessionFactory();
  }

  public <T> T inTransaction(Callable<T> call) {
    return daoTest.inTransaction(call);
  }

  public void inTransaction(Runnable action) {
    daoTest.inTransaction(action);
  }
}
