package org.sdase.commons.shared.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ApiInvalidParamTest {

  @Test
  public void toStringShouldNotContainReason() {
    ApiInvalidParam value = new ApiInvalidParam("field", "reason", "errorCode");
    assertThat(value.toString()).contains("field", "errorCode").doesNotContain("reason");
  }
}
