package org.sdase.commons.server.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response.Status;
import org.junit.Test;

public class JavaxWsRsJava11Test {

  /** This test makes sure RS API 2 is in the classpath. */
  @Test
  public void shouldBeAbleToImportClassFromRsApi2() {
    assertThat(Status.REQUEST_TIMEOUT).isNotNull();
  }
}
