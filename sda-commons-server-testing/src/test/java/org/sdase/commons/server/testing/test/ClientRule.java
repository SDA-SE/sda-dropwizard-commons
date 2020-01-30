package org.sdase.commons.server.testing.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ClientRule implements TestRule {
  private int port;

  public ClientRule(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return base;
  }
}
