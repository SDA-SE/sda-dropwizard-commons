package org.sdase.commons.server.errorhandling.rest.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.sdase.commons.server.errorhandling.rest.validation.UpperCase;

/** Dummy object for request to get {@link javax.validation.ValidationException} */
public class RequestObject {

  @NotEmpty @UpperCase private String param1;

  public String getParam1() {
    return param1;
  }

  public void setParam1(String param1) {
    this.param1 = param1;
  }
}
