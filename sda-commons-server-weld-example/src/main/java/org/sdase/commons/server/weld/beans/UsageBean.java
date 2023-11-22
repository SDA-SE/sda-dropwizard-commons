package org.sdase.commons.server.weld.beans;

import jakarta.inject.Inject;

public class UsageBean {

  private SimpleBean simpleBean;

  @Inject
  public UsageBean(SimpleBean simpleBean) {
    this.simpleBean = simpleBean;
  }

  public void useSimpleBean() {
    simpleBean.doStuff();
  }
}
