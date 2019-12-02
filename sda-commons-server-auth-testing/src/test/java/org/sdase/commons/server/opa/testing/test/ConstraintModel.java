package org.sdase.commons.server.opa.testing.test;

import java.util.List;
import java.util.Map;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;

public class ConstraintModel {

  private boolean fullAccess;

  private MultivaluedStringMap constraint;

  public boolean isFullAccess() {
    return fullAccess;
  }

  public ConstraintModel setFullAccess(boolean fullAccess) {
    this.fullAccess = fullAccess;
    return this;
  }

  public Map<String, List<String>> getConstraint() {
    return constraint;
  }

  public ConstraintModel setConstraint(
      MultivaluedStringMap constraint) {
    this.constraint = constraint;
    return this;
  }

  public ConstraintModel addConstraint(String name, String ... value) {
    if (constraint == null) {
      constraint = new MultivaluedStringMap();
    }
    constraint.addAll(name, value);
    return this;
  }
}
