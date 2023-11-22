package org.sdase.commons.server.weld.beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SimpleBean {

  @Inject private String someString;

  private static final Logger LOG = LoggerFactory.getLogger(SimpleBean.class);

  public void doStuff() {
    LOG.info("do stuff invoked");
    LOG.info("injected string '{}'", someString);
  }
}
