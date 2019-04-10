package org.sdase.commons.server.opa.testing.test;

@SuppressWarnings("WeakerAccess")
public class PrincipalInfo {

  private String name;
  private String jwt;
  private ConstraintModel constraints;

  public String getName() {
    return name;
  }

  public PrincipalInfo setName(String name) {
    this.name = name;
    return this;
  }

  public String getJwt() {
    return jwt;
  }

  public PrincipalInfo setJwt(String jwt) {
    this.jwt = jwt;
    return this;
  }

  public ConstraintModel getConstraints() {
    return constraints;
  }

  public PrincipalInfo setConstraints(ConstraintModel constraints) {
    this.constraints = constraints;
    return this;
  }
}
