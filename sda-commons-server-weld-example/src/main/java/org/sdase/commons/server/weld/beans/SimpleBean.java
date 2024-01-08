package org.sdase.commons.server.weld.beans;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBean {

  @Inject
  @Named("some-string")
  private String someString;

  private static final Logger LOG = LoggerFactory.getLogger(SimpleBean.class);

  public void doStuff() {
    LOG.info("do stuff invoked");
    LOG.info("injected string '{}'", someString);
  }
}
