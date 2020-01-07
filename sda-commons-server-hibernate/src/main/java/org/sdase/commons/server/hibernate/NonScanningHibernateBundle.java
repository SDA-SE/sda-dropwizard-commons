package org.sdase.commons.server.hibernate;

import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import java.util.List;

/**
 * Simple implementation of a {@link HibernateBundle} that uses direct configuration and not package
 * scanning.
 *
 * @param <T> the configuration type
 */
public abstract class NonScanningHibernateBundle<T extends Configuration> extends HibernateBundle<T> {

  public NonScanningHibernateBundle(List<Class<?>> entities,
      SessionFactoryFactory sessionFactoryFactory) {
    super(
        new ImmutableList.Builder<Class<?>>().addAll(entities.iterator()).build(),
        sessionFactoryFactory
    );
  }
}
