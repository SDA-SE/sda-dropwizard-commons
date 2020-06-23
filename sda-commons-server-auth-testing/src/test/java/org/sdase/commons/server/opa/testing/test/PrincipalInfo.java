package org.sdase.commons.server.opa.testing.test;

@SuppressWarnings("WeakerAccess")
public class PrincipalInfo {

  private String name;
  private String jwt;
  private ConstraintModel constraints;
  private String constraintsJson;
  private String sub;

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

  public String getConstraintsJson() {
    return constraintsJson;
  }

  public PrincipalInfo setConstraintsJson(String constraintsJson) {
    this.constraintsJson = constraintsJson;
    return this;
  }

  public String getSub() {
    return sub;
  }

  public PrincipalInfo setSub(String sub) {
    this.sub = sub;
    return this;
  }
}
